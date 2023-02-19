package com.github.zxbu.webdavteambition.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zxbu.webdavteambition.client.AliYunDriverClient;
import com.github.zxbu.webdavteambition.config.AliYunDriveProperties;
import com.github.zxbu.webdavteambition.model.*;
import com.github.zxbu.webdavteambition.model.result.TFile;
import com.github.zxbu.webdavteambition.model.result.TFileCacheKeyInfo;
import com.github.zxbu.webdavteambition.model.result.TFileListResult;
import com.github.zxbu.webdavteambition.model.result.UploadPreResult;
import com.github.zxbu.webdavteambition.util.JsonUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.sf.webdav.exceptions.WebdavException;

import okhttp3.HttpUrl;
import okhttp3.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AliYunDriverClientService {

    private static AliYunDriverClientService sInstance;
    public static AliYunDriverClientService getInstance() {
        if (sInstance == null) {
            synchronized (AliYunDriverClientService.class) {
                if (sInstance == null) {
                    String workDir;
                    File workDirLinux = new File("/root/data");
                    if (workDirLinux.exists()) {
                        workDir = workDirLinux.getAbsolutePath() + File.separator;
                    } else {
                        workDir = AliYunDriverClientService.class.getClassLoader().getResource(".").getPath() + File.separator;
                    }

                    AliYunDriveProperties properties = AliYunDriveProperties.load(workDir);
                    properties.authorization = null;
                    properties.refreshTokenNext = System.getProperty("REFRESH_TOKEN");
                    properties.deviceName = System.getProperty("DEVICE_NAME");
                    properties.workDir = workDir;
                    if (StringUtils.isEmpty(properties.deviceId)) {
                        properties.deviceId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
                        properties.save();
                    }
                    sInstance = new AliYunDriverClientService(new AliYunDriverClient(properties));
                }
            }
        }
        return sInstance;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AliYunDriverClientService.class);
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static String rootPath = "/";
    private static int chunkSize = 10485760; // 10MB
    private TFile rootTFile = null;
    private final Pattern shareNamePatten = Pattern.compile("^.*!(?<shareId>[a-zA-Z0-9]{11})(?>$|:(?<password>[a-zA-Z0-9]{4})$)");

    private static LoadingCache<String, Set<TFile>> tFilesCache = CacheBuilder.newBuilder()
            .initialCapacity(128)
            .maximumSize(1024)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Set<TFile>>() {
                @Override
                public Set<TFile> load(String jsonKey) throws Exception {
                    TFileCacheKeyInfo info = JsonUtil.readValue(jsonKey, TFileCacheKeyInfo.class);
                    return AliYunDriverClientService.getInstance().getTFiles2(info.nodeId, info.shareId, info.sharePassword);
                }
            });

    private static LoadingCache<String, String> shareLinkDownloadUrlCache = CacheBuilder.newBuilder()
            .initialCapacity(128)
            .maximumSize(1024)
            .expireAfterWrite(500, TimeUnit.SECONDS)
            .build(new CacheLoader<>() {
                @Override
                public String load(String key) throws Exception {
                    return null;
                }
            });

    public final AliYunDriverClient client;

    private VirtualTFileService virtualTFileService = VirtualTFileService.getInstance();

    public AliYunDriverClientService(AliYunDriverClient aliYunDriverClient) {
        this.client = aliYunDriverClient;
        AliYunDriverFileSystemStore.setBean(this);
    }

    public Set<TFile> getTFiles(String nodeId, String shareId, String sharePassword) {
        try {
            String jsonKey = JsonUtil.toJson(new TFileCacheKeyInfo(nodeId, shareId, sharePassword));
            Set<TFile> tFiles = tFilesCache.get(jsonKey, () -> {
                // 获取真实的文件列表
                return getTFiles2(nodeId, shareId, sharePassword);
            });
            Set<TFile> all = new LinkedHashSet<>(tFiles);
            // 获取上传中的文件列表
            Collection<TFile> virtualTFiles = virtualTFileService.list(nodeId);
            all.addAll(virtualTFiles);
            return all;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<TFile> getTFiles2(String nodeId, String shareId, String sharePassword) {
        List<TFile> tFileList = fileListFromApi(nodeId, null, new ArrayList<>(), shareId, sharePassword);
        tFileList.sort(Comparator.comparing(TFile::getUpdated_at).reversed());
        Set<TFile> tFileSets = new LinkedHashSet<>();
        for (TFile tFile : tFileList) {
            tFile.setShare_password(sharePassword);
            if (!tFileSets.add(tFile)) {
                LOGGER.info("当前目录下{} 存在同名文件：{}，文件大小：{}", nodeId, tFile.getName(), tFile.getSize());
            }
        }
        // 对文件名进行去重，只保留最新的一个
        return tFileSets;
    }

    private List<TFile> fileListFromApi(String nodeId, String marker, List<TFile> all, String shareId, String sharePassword) {
        FileListRequest listQuery = new FileListRequest();
        listQuery.setMarker(marker);
        listQuery.setLimit(200);
        listQuery.setOrder_by("updated_at");
        listQuery.setOrder_direction("DESC");
        listQuery.setParent_file_id(nodeId);
        if (shareId != null){
            listQuery.setShare_id(shareId);
        } else {
            listQuery.setDrive_id(client.getDriveId());
        }
        String json;
        try {
            json = client.post("/file/list", listQuery, shareId, sharePassword);
        } catch (WebdavException e){
            // 无法获取列表可能是 shareId 已经失效，与其报错不如返回空，特别是使用 dav 挂载时候，报错会导致 IO 错误无法删除目录
            e.printStackTrace();
            return Collections.emptyList();
        }
        TFileListResult<TFile> tFileListResult = JsonUtil.readValue(json, new TypeReference<TFileListResult<TFile>>() {});
        all.addAll(tFileListResult.getItems());
        if (StringUtils.isEmpty(tFileListResult.getNext_marker())) {
            return all;
        }
        return fileListFromApi(nodeId, tFileListResult.getNext_marker(), all, shareId, sharePassword);
    }

    @Nullable
    public TFile fileGet(String nodeId, String shareId, String sharePassword) {
        return fileGet(nodeId, shareId, sharePassword, -1);
    }

    @Nullable
    public TFile fileGet(String nodeId, String shareId, String sharePassword, long thumbnail_time_ms) {
        FileGetRequest query = new FileGetRequest();
        query.setFile_id(nodeId);
        if (StringUtils.isEmpty(shareId)) {
            query.setDrive_id(client.getDriveId());
        } else {
            query.setShare_id(shareId);
        }
        if (thumbnail_time_ms > -1) {
            query.set_thumbnail_time_ms(thumbnail_time_ms);
        }
        try {
            String json = client.post("/file/get", query, shareId, sharePassword);
            return JsonUtil.readValue(json, TFile.class);
        } catch (WebdavException e){
            // 无法获取列表可能是 shareId 已经失效，与其报错不如返回空，特别是使用 dav 挂载时候，报错会导致 IO 错误无法删除目录
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, String> toMap(Object o) {
        try {
            String json = objectMapper.writeValueAsString(o);
            Map<String, Object> rawMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            Map<String, String> stringMap = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                String s = entry.getKey();
                Object o1 = entry.getValue();
                if (o1 == null) {
                    continue;
                }
                stringMap.put(s, o1.toString());
            }
            return stringMap;
        } catch (Exception e) {
            throw new WebdavException(e);
        }
    }

    public void uploadPre(String path, long size, InputStream inputStream) {
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        TFile parent = getTFileByPath(pathInfo.getParentPath());
        if (parent == null) {
            return;
        }
        // 如果已存在，先删除
        TFile tfile = getTFileByPath(path);
        if (tfile != null) {
            if (tfile.getSize() == size) {
                //如果文件大小一样，则不再上传
                return;
            }
            remove(path);
        }


        int chunkCount = (int) Math.ceil(((double) size) / chunkSize); // 进1法

        UploadPreRequest uploadPreRequest = new UploadPreRequest();
//        uploadPreRequest.setContent_hash(UUID.randomUUID().toString());
        uploadPreRequest.setDrive_id(client.getDriveId());
        uploadPreRequest.setName(pathInfo.getName());
        uploadPreRequest.setParent_file_id(parent.getFile_id());
        uploadPreRequest.setSize(size);
        List<UploadPreRequest.PartInfo> part_info_list = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            UploadPreRequest.PartInfo partInfo = new UploadPreRequest.PartInfo();
            partInfo.setPart_number(i + 1);
            part_info_list.add(partInfo);
        }
        uploadPreRequest.setPart_info_list(part_info_list);

        LOGGER.info("开始上传文件，文件名：{}，总大小：{}, 文件块数量：{}", path, size, chunkCount);

        String json = client.post("/file/create_with_proof", uploadPreRequest);
        UploadPreResult uploadPreResult = JsonUtil.readValue(json, UploadPreResult.class);
        List<UploadPreRequest.PartInfo> partInfoList = uploadPreResult.getPart_info_list();
        if (partInfoList != null) {
            if (size > 0) {
                virtualTFileService.createTFile(parent.getFile_id(), uploadPreResult);
            }
            LOGGER.info("文件预处理成功，开始上传。文件名：{}，上传URL数量：{}", path, partInfoList.size());

            byte[] buffer = new byte[chunkSize];
            for (int i = 0; i < partInfoList.size(); i++) {
                UploadPreRequest.PartInfo partInfo = partInfoList.get(i);

                long expires = Long.parseLong(Objects.requireNonNull(Objects.requireNonNull(HttpUrl.parse(partInfo.getUpload_url())).queryParameter("x-oss-expires")));
                if (System.currentTimeMillis() / 1000 + 10 >= expires) {
                    // 已过期，重新置换UploadUrl
                    RefreshUploadUrlRequest refreshUploadUrlRequest = new RefreshUploadUrlRequest();
                    refreshUploadUrlRequest.setDrive_id(client.getDriveId());
                    refreshUploadUrlRequest.setUpload_id(uploadPreResult.getUpload_id());
                    refreshUploadUrlRequest.setFile_id(uploadPreResult.getFile_id());
                    refreshUploadUrlRequest.setPart_info_list(part_info_list);
                    String refreshJson = client.post("/file/get_upload_url", refreshUploadUrlRequest);
                    UploadPreResult refreshResult = JsonUtil.readValue(refreshJson, UploadPreResult.class);
                    for (int j = i; j < partInfoList.size(); j++) {
                        UploadPreRequest.PartInfo oldInfo = partInfoList.get(j);
                        UploadPreRequest.PartInfo newInfo = null;
                        List<UploadPreRequest.PartInfo> list = refreshResult.getPart_info_list();
                        for (UploadPreRequest.PartInfo p : list) {
                            if (p.getPart_number().equals(oldInfo.getPart_number())) {
                                newInfo = p;
                                break;
                            }
                        }
                        if (newInfo == null) {
                            throw new NullPointerException("newInfo is null");
                        }
                        oldInfo.setUpload_url(newInfo.getUpload_url());
                    }
                }

                try {
                    int read = IOUtils.read(inputStream, buffer, 0, buffer.length);
                    if (read == -1) {
                        LOGGER.info("文件上传结束。文件名：{}，当前进度：{}/{}", path, (i + 1), partInfoList.size());
                        return;
                    }
                    client.upload(partInfo.getUpload_url(), buffer, 0, read);
                    virtualTFileService.updateLength(parent.getFile_id(), uploadPreResult.getFile_id(), buffer.length);
                    LOGGER.info("文件正在上传。文件名：{}，当前进度：{}/{}", path, (i + 1), partInfoList.size());
                } catch (IOException e) {
                    virtualTFileService.remove(parent.getFile_id(), uploadPreResult.getFile_id());
                    throw new WebdavException(e);
                }
            }
        }



        UploadFinalRequest uploadFinalRequest = new UploadFinalRequest();
        uploadFinalRequest.setFile_id(uploadPreResult.getFile_id());
        uploadFinalRequest.setDrive_id(client.getDriveId());
        uploadFinalRequest.setUpload_id(uploadPreResult.getUpload_id());

        client.post("/file/complete", uploadFinalRequest);
        virtualTFileService.remove(parent.getFile_id(), uploadPreResult.getFile_id());
        LOGGER.info("文件上传成功。文件名：{}", path);
        clearCache();
    }


    public void rename(String sourcePath, String newName) {
        sourcePath = normalizingPath(sourcePath);
        TFile tFile = getTFileByPath(sourcePath);
        RenameRequest renameRequest = new RenameRequest();
        renameRequest.setDrive_id(client.getDriveId());
        renameRequest.setFile_id(tFile.getOrigin_file_id() != null ? tFile.getOrigin_file_id() : tFile.getFile_id());
        renameRequest.setName(newName);
        try {
            client.post("/file/update", renameRequest);
        } catch (WebdavException e) {
            String res = e.responseMessage;
            if (StringUtils.isEmpty(res)) {
                throw e;
            }
            if (!res.contains("AlreadyExist.File")) {
                throw e;
            }
            remove(getNodeIdByParentId(tFile.getParent_file_id(), newName, tFile.getShare_id(), tFile.getShare_password()));
            client.post("/file/update", renameRequest);
        }
        clearCache();
    }

    public void move(String sourcePath, String targetPath) {
        sourcePath = normalizingPath(sourcePath);
        targetPath = normalizingPath(targetPath);

        TFile sourceTFile = getTFileByPath(sourcePath);
        TFile targetTFile = getTFileByPath(targetPath);
        MoveRequest moveRequest = new MoveRequest();
        moveRequest.setDrive_id(client.getDriveId());
        moveRequest.setFile_id(sourceTFile.getOrigin_file_id() != null ? sourceTFile.getOrigin_file_id() : sourceTFile.getFile_id());
        moveRequest.setTo_parent_file_id(targetTFile.getFile_id());
        client.post("/file/move", moveRequest);
        clearCache();
    }

    public void remove(String path) {
        path = normalizingPath(path);
        TFile tFile = getTFileByPath(path);
        remove(tFile);
    }

    public void remove(TFile tFile) {
        if (tFile == null) {
            return;
        }
        RemoveRequest removeRequest = new RemoveRequest();
        removeRequest.setDrive_id(client.getDriveId());
        removeRequest.setFile_id(tFile.getOrigin_file_id() != null ? tFile.getOrigin_file_id() : tFile.getFile_id());
        client.post("/recyclebin/trash", removeRequest);
        clearCache();
    }


    public void createFolder(String path) {
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        TFile parent =  getTFileByPath(pathInfo.getParentPath());
        if (parent == null) {
            LOGGER.warn("创建目录失败，未发现父级目录：{}", pathInfo.getParentPath());
            return;
        }

        CreateFileRequest createFileRequest = new CreateFileRequest();
        createFileRequest.setDrive_id(client.getDriveId());
        createFileRequest.setName(pathInfo.getName());
        createFileRequest.setParent_file_id(parent.getFile_id());
        createFileRequest.setType(FileType.folder.name());
        String json = client.post("/file/create_with_proof", createFileRequest);
        TFile createFileResult = JsonUtil.readValue(json, TFile.class);
        if (createFileResult.getFile_name() == null) {
            LOGGER.error("创建目录{}失败: {}",path, json);
        }
        if (!createFileResult.getFile_name().equals(pathInfo.getName())) {
            LOGGER.info("创建目录{}与原值{}不同，重命名", createFileResult.getName(), pathInfo.getName());
            rename(pathInfo.getParentPath() + "/" + createFileResult.getName(), pathInfo.getName());
            clearCache();
        }
        clearCache();
    }


    public TFile getTFileByPath(String path) {
        path = normalizingPath(path);

        return getNodeIdByPath2(path);
    }

    public Response download(String path, HttpServletRequest request, long size ) {
        TFile file = getTFileByPath(path);
        if (file.getShare_id() == null){
            DownloadRequest downloadRequest = new DownloadRequest();
            downloadRequest.setDrive_id(client.getDriveId());
            downloadRequest.setFile_id(file.getFile_id());
            String json = client.post("/file/get_download_url", downloadRequest);
            Object url = JsonUtil.getJsonNodeValue(json, "url");
            LOGGER.debug("{} url = {}", path, url);
            return client.download(url.toString(), request, size);
        } else {
            DownloadRequest downloadRequest = new DownloadRequest();
            downloadRequest.setFile_id(file.getFile_id());
            downloadRequest.setShare_id(file.getShare_id());
            downloadRequest.setShareToken(client.readShareToken(file.getShare_id(), file.getShare_password()));
            downloadRequest.setExpire_sec(600);
            String url = null;
            try {
                url = shareLinkDownloadUrlCache.get(file.getShare_id() + ":" + file.getShare_password(), new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        String json = client.post("/file/get_share_link_download_url", downloadRequest, file.getShare_id(), file.getShare_password());
                        Object url = JsonUtil.getJsonNodeValue(json, "url");
                        return url.toString();
                    }
                });
            } catch (Exception e) {

            }
            if (StringUtils.isEmpty(url)) {
                String json = client.post("/file/get_share_link_download_url", downloadRequest, file.getShare_id(), file.getShare_password());
                url = JsonUtil.getJsonNodeValue(json, "url").toString();
            }

            LOGGER.debug("{} url = {}", path, url);
            return client.download(url.replaceAll("^https://", "http://"), request, size);
        }
    }

    private TFile getNodeIdByPath2(String path) {
        if (StringUtils.isEmpty(path)) {
            path = rootPath;
        }
        if (path.equals(rootPath)) {
            return getRootTFile();
        }
        PathInfo pathInfo = getPathInfo(path);
        TFile tFile = getTFileByPath(pathInfo.getParentPath());
        if (tFile == null ) {
            return null;
        }
        return getNodeIdByParentId(tFile.getFile_id(), pathInfo.getName(), tFile.getShare_id(), tFile.getShare_password());
    }


    public PathInfo getPathInfo(String path) {
        path = normalizingPath(path);
        if (path.equals(rootPath)) {
            PathInfo pathInfo = new PathInfo();
            pathInfo.setPath(path);
            pathInfo.setName(path);
            return pathInfo;
        }
        int index = path.lastIndexOf("/");
        String parentPath = path.substring(0, index + 1);
        String name = path.substring(index+1);
        PathInfo pathInfo = new PathInfo();
        pathInfo.setPath(path);
        pathInfo.setParentPath(parentPath);
        pathInfo.setName(name);
        return pathInfo;
    }

    private TFile getRootTFile() {
        if (rootTFile == null) {
//            FileGetRequest fileGetRequest = new FileGetRequest();
//            fileGetRequest.setFile_id("root");
//            fileGetRequest.setDrive_id(client.getDriveId());
//            String json = client.post("/file/get", fileGetRequest);
//            rootTFile = JsonUtil.readValue(json, TFile.class);
            rootTFile = new TFile();
            rootTFile.setName("/");
            rootTFile.setFile_id("root");
            rootTFile.setCreated_at(new Date());
            rootTFile.setUpdated_at(new Date());
            rootTFile.setType("folder");
        }
        return rootTFile;
    }

    private TFile getShareRootTFile(String shareId, String name, String originFileId, String sharePassword){
        TFile rootTFile = new TFile();
        rootTFile.setName(name);
        rootTFile.setFile_id("root");
        rootTFile.setCreated_at(new Date());
        rootTFile.setUpdated_at(new Date());
        rootTFile.setType("folder");
        rootTFile.setShare_id(shareId);
        rootTFile.setOrigin_file_id(originFileId);
        rootTFile.setShare_password(sharePassword);
        return rootTFile;
    }

    public TFile getNodeIdByParentId(String parentId, String name, String shareId, String sharePassword) {
        Set<TFile> tFiles = getTFiles(parentId, shareId, sharePassword);
        for (TFile tFile : tFiles) {
            Matcher matcher = shareNamePatten.matcher(name);
            if (matcher.find()){
                return getShareRootTFile(matcher.group("shareId"), name, tFile.getFile_id(), matcher.group("password"));
            } else {
                if (tFile.getName().equals(name)) {
                    return tFile;
                }
            }
        }
        return null;
    }


    private String normalizingPath(String path) {
        path = path.replaceAll("//", "/");
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void clearCache() {
        tFilesCache.invalidateAll();
    }
}

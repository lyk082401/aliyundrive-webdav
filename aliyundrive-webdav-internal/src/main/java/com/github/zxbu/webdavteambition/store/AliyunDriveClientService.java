package com.github.zxbu.webdavteambition.store;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.manager.AliyunDriveSessionManager;
import com.github.zxbu.webdavteambition.model.PathInfo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.sf.webdav.exceptions.WebdavException;
import net.xdow.aliyundrive.AliyunDrive;
import net.xdow.aliyundrive.AliyunDriveConstant;
import net.xdow.aliyundrive.IAliyunDrive;
import net.xdow.aliyundrive.IAliyunDriveAuthorizer;
import net.xdow.aliyundrive.bean.*;
import net.xdow.aliyundrive.exception.NotAuthenticatedException;
import net.xdow.aliyundrive.net.AliyunDriveCall;
import net.xdow.aliyundrive.util.JsonUtils;
import net.xdow.aliyundrive.webapi.AliyunDriveWebConstant;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AliyunDriveClientService<T extends IAliyunDrive> implements IAliyunDriveAuthorizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunDriveClientService.class);
    private static String rootPath = "/";
    private static int chunkSize = 10485760; // 10MB
    private AliyunDriveFileInfo rootTFile = null;

    private AliyunDriveResponse.UserDriveInfo mUserDriveInfo;

    private AliyunDriveProperties mAliyunDriveProperties;
    // / 字符占位符
    private static String FILE_PATH_PLACE_HOLDER = "[@-@]";
    private LoadingCache<String, Set<AliyunDriveFileInfo>> tFilesCache = CacheBuilder.newBuilder()
            .initialCapacity(128)
            .maximumSize(10240)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, Set<AliyunDriveFileInfo>>() {
                @Override
                public Set<AliyunDriveFileInfo> load(String key) throws Exception {
                    return AliyunDriveClientService.this.getTFiles2(key);
                }
            });

    private LoadingCache<String, AliyunDriveResponse.FileGetDownloadUrlInfo> tFileDownloadUrlCache = CacheBuilder.newBuilder()
            .initialCapacity(128)
            .maximumSize(10240)
            .expireAfterWrite(AliyunDriveConstant.MAX_DOWNLOAD_URL_EXPIRE_TIME_SEC, TimeUnit.SECONDS)
            .build(new CacheLoader<String, AliyunDriveResponse.FileGetDownloadUrlInfo>() {
                @Override
                public AliyunDriveResponse.FileGetDownloadUrlInfo load(String key) throws Exception {
                    return fileGetDownloadUrlInternal(key);
                }
            });
    private final T mAliyunDrive;

    public AliyunDriveClientService(Class<? extends IAliyunDrive> aliyunDriveCls, AliyunDriveProperties aliyunDriveProperties) {
        System.setProperty("dns.server", "223.5.5.5,114.114.114.114");
        this.mAliyunDriveProperties = aliyunDriveProperties;
        this.mAliyunDrive = (T) AliyunDrive.newAliyunDrive(aliyunDriveCls);
        this.mAliyunDrive.setAuthorizer(this);
        loginAsync(this.mAliyunDriveProperties.getRefreshToken(), new Runnable() {
            @Override
            public void run() {
                loginAsync(AliyunDriveClientService.this.mAliyunDriveProperties.getRefreshTokenNext());
            }
        });
    }

    public AliyunDriveProperties getProperties() {
        return this.mAliyunDriveProperties;
    }

    public T getAliyunDrive() {
        return this.mAliyunDrive;
    }

    public Set<AliyunDriveFileInfo> getTFiles(final String fileId) {
        try {
            Set<AliyunDriveFileInfo> tFiles = tFilesCache.get(fileId);
            Set<AliyunDriveFileInfo> all = new LinkedHashSet<>(tFiles);
            // 获取上传中的文件列表
            Collection<AliyunDriveFileInfo> virtualTFiles = VirtualTFileService.getInstance().list(fileId);
            all.addAll(virtualTFiles);
            return all;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<AliyunDriveFileInfo> getTFiles2(String nodeId) {
        List<AliyunDriveFileInfo> tFileList = fileListFromApi(nodeId, null, new ArrayList<AliyunDriveFileInfo>());
        Collections.sort(tFileList, new Comparator<AliyunDriveFileInfo>() {
            @Override
            public int compare(AliyunDriveFileInfo o1, AliyunDriveFileInfo o2) {
                return o2.getUpdatedAt().compareTo(o1.getUpdatedAt());
            }
        });
        Set<AliyunDriveFileInfo> tFileSets = new LinkedHashSet<>();
        for (AliyunDriveFileInfo tFile : tFileList) {
            String fileName = tFile.getName();
            if (StringUtils.isNotEmpty(fileName) && fileName.contains("/")) {
                tFile.setName(fileName.replace("/", FILE_PATH_PLACE_HOLDER));
            }
            if (!tFileSets.add(tFile)) {
                LOGGER.info("当前目录下{} 存在同名文件：{}，文件大小：{}", nodeId, tFile.getName(), tFile.getSize());
            }
        }
        // 对文件名进行去重，只保留最新的一个
        return tFileSets;
    }

    private List<AliyunDriveFileInfo> fileListFromApi(String nodeId, String marker, List<AliyunDriveFileInfo> all) {
        try {
        AliyunDriveRequest.FileListInfo query = new AliyunDriveRequest.FileListInfo(
                getDefaultDriveId(), nodeId
        );
        query.setMarker(marker);
        query.setLimit(200);
        query.setOrderBy(AliyunDriveEnum.OrderBy.UpdatedAt);
        query.setOrderDirection(AliyunDriveEnum.OrderDirection.Desc);
            AliyunDriveResponse.FileListInfo res = this.mAliyunDrive.fileList(query).execute();
            if (res.isError()) {
                if ("TooManyRequests".equals(res.getCode())) {
                    try {
                        TimeUnit.SECONDS.sleep(6);
                    } catch (InterruptedException e) {
                    }
                    res = this.mAliyunDrive.fileList(query).execute();
                }
            }
            if (res.isError()) {
                if ("TooManyRequests".equals(res.getCode())) {
                    try {
                        TimeUnit.SECONDS.sleep(6);
                    } catch (InterruptedException e) {
                    }
                    res = this.mAliyunDrive.fileList(query).execute();
                }
            }
            all.addAll(res.getItems());
            String nextMarker = res.getNextMarker();
            if (StringUtils.isEmpty(nextMarker)) {
                return all;
            }
            return fileListFromApi(nodeId, nextMarker, all);
        } catch (NotAuthenticatedException e) {
            //WebApi需要这个来引导用户输入token
            if (this.mAliyunDrive instanceof AliyunDriveWebApiImplV1) {
                return new ArrayList<>();
            }
            throw e;
        }
    }

    @Nullable
    private String getDefaultDriveId() {
        AliyunDriveResponse.UserDriveInfo info = getUserDriveInfo();
        if (info == null) {
            return null;
        }
        return info.getDefaultDriveId();
    }

    @Nullable
    private AliyunDriveResponse.UserDriveInfo getUserDriveInfo() {
        if (mUserDriveInfo == null) {
            synchronized (AliyunDriveClientService.class) {
                if (mUserDriveInfo == null) {
                    AliyunDriveResponse.UserDriveInfo res = mAliyunDrive.getUserDriveInfo().execute();
                    if (!res.isError()) {
                        mUserDriveInfo = res;
                    }
                }
            }
        }
        return mUserDriveInfo;
    }

    public void uploadPre(String path, long size, InputStream inputStream, JapHttpResponse response) {
        VirtualTFileService virtualTFileService = VirtualTFileService.getInstance();
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        AliyunDriveFileInfo parent = getTFileByPath(pathInfo.getParentPath());
        if (parent == null) {
            return;
        }
        try {
            // 如果已存在，先删除
            AliyunDriveFileInfo tfile = getTFileByPath(path);
            if (tfile != null) {
                if (tfile.getSize() == size) {
                    //如果文件大小一样，则不再上传
                    return;
                }
                remove(path);
            }

            AliyunDriveResponse.FileCreateInfo fileCreateInfo = uploadCreateFile(parent.getFileId(), pathInfo.getName(), size);
            String fileId = fileCreateInfo.getFileId();
            String uploadId = fileCreateInfo.getUploadId();
            List<AliyunDriveFilePartInfo> partInfoList = fileCreateInfo.getPartInfoList();
            try {
                long totalUploadedSize = 0;
                if (partInfoList != null) {
                    if (size > 0) {
                        virtualTFileService.createVirtualFile(parent.getFileId(), fileCreateInfo);
                    }
                    LOGGER.info("文件预处理成功，开始上传。文件名：{}，上传URL数量：{}", path, partInfoList.size());

                    byte[] buffer = new byte[chunkSize];
                    for (int i = 0; i < partInfoList.size(); i++) {
                        AliyunDriveFilePartInfo partInfo = partInfoList.get(i);

                        long expires = Long.parseLong(Objects.requireNonNull(Objects.requireNonNull(HttpUrl.parse(partInfo.getUploadUrl())).queryParameter("x-oss-expires")));
                        if (System.currentTimeMillis() / 1000 + 10 >= expires) {
                            // 已过期，重新置换UploadUrl
                            refreshUploadUrl(fileId, uploadId, partInfoList);
                        }

                        try {
                            int read = IOUtils.read(inputStream, buffer, 0, buffer.length);
                            if (read == -1) {
                                LOGGER.info("文件上传结束。文件名：{}，当前进度：{}/{}", path, (i + 1), partInfoList.size());
                                return;
                            } else if (read == 0) {
                                continue;
                            }
                            this.mAliyunDrive.upload(partInfo.getUploadUrl(), buffer, 0, read).execute();
                            virtualTFileService.updateLength(parent.getFileId(), fileId, buffer.length);
                            LOGGER.info("文件正在上传。文件名：{}，当前进度：{}/{}", path, (i + 1), partInfoList.size());
                            response.flushBuffer();
                            totalUploadedSize += read;
                        } catch (IOException e) {
                            throw new WebdavException(e);
                        }
                    }
                }
                if (totalUploadedSize == size) {
                    uploadComplete(fileId, uploadId);
                    LOGGER.info("文件上传成功。文件名：{}", path);
                } else {
                    LOGGER.info("文件上传失败。文件名：{} 文件大小: {} 已上传: {}", path, size, totalUploadedSize);
                }
            } finally {
                virtualTFileService.remove(parent.getFileId(), fileId);
                clearCache(fileId);
            }
        } finally {
            clearCache(parent.getFileId());
        }
    }

    private void uploadComplete(String fileId, String uploadId) {
        AliyunDriveRequest.FileUploadCompleteInfo query = new AliyunDriveRequest.FileUploadCompleteInfo(
                getDefaultDriveId(), fileId, uploadId
        );
        AliyunDriveResponse.FileUploadCompleteInfo res = this.mAliyunDrive.fileUploadComplete(query).execute();
        if (!StringUtils.isEmpty(res.getCode())) {
            throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
        }
    }

    private void refreshUploadUrl(String fileId, String uploadId, List<AliyunDriveFilePartInfo> partInfoList) {
        AliyunDriveRequest.FileGetUploadUrlInfo query = new AliyunDriveRequest.FileGetUploadUrlInfo(
                getDefaultDriveId(), fileId, uploadId, partInfoList
        );
        AliyunDriveResponse.FileGetUploadUrlInfo res = this.mAliyunDrive.fileGetUploadUrl(query).execute();
        List<AliyunDriveFilePartInfo> newPartInfoList = res.getPartInfoList();
        Map<Long, AliyunDriveFilePartInfo> newPartInfoMap = new HashMap<>();
        for (AliyunDriveFilePartInfo partInfo : newPartInfoList) {
            newPartInfoMap.put(partInfo.getPartNumber(), partInfo);
        }

        for (int j = 0; j < partInfoList.size(); j++) {
            AliyunDriveFilePartInfo oldInfo = partInfoList.get(j);
            AliyunDriveFilePartInfo newInfo = newPartInfoMap.get(oldInfo.getPartNumber());
            if (newInfo == null) {
                throw new NullPointerException("newInfo is null");
            }
            oldInfo.setUploadUrl(newInfo.getUploadUrl());
        }
    }

    private AliyunDriveResponse.FileCreateInfo uploadCreateFile(String parentFileId, String name, long size) {
        AliyunDriveRequest.FileCreateInfo query = new AliyunDriveRequest.FileCreateInfo(
                getDefaultDriveId(), parentFileId, name, AliyunDriveEnum.Type.File,
                AliyunDriveEnum.CheckNameMode.Refuse
        );
        query.setSize(size);
        int chunkCount = (int) Math.ceil(((double) size) / chunkSize); // 进1法
        List<AliyunDriveFilePartInfo> partInfoList = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            AliyunDriveFilePartInfo partInfo = new AliyunDriveFilePartInfo();
            partInfo.setPartNumber(i + 1);
            partInfoList.add(partInfo);
        }
        query.setPartInfoList(partInfoList);
        LOGGER.info("开始上传文件，文件名：{}，总大小：{}, 文件块数量：{}", name, size, chunkCount);
        AliyunDriveResponse.FileCreateInfo res = this.mAliyunDrive.fileCreate(query).execute();
        if (!StringUtils.isEmpty(res.getCode())) {
            throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
        }
        return res;
    }


    public void rename(String sourcePath, String newName) {
        sourcePath = normalizingPath(sourcePath);
        AliyunDriveFileInfo tFile = getTFileByPath(sourcePath);
        try {
            AliyunDriveRequest.FileRenameInfo query = new AliyunDriveRequest.FileRenameInfo(
                    getDefaultDriveId(), tFile.getFileId(), newName, tFile.getParentFileId()
            );

            AliyunDriveResponse.FileRenameInfo res = this.mAliyunDrive.fileRename(query).execute();
            if (res.isError()) {
                if ("AlreadyExist.File".equals(res.getCode())) {
                    remove(getNodeIdByParentId(tFile.getParentFileId(), newName));
                    res = this.mAliyunDrive.fileRename(query).execute();
                }
            }
            try {
                if (!StringUtils.isEmpty(res.getCode())) {
                    throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
                }
            } finally {
                clearCache(res.getFileId());
            }
        } finally {
            clearCache(tFile.getFileId());
            clearCache(tFile.getParentFileId());
        }
    }

    public void move(String sourcePath, String targetPath) {
        sourcePath = normalizingPath(sourcePath);
        targetPath = normalizingPath(targetPath);

        AliyunDriveFileInfo sourceTFile = getTFileByPath(sourcePath);
        AliyunDriveFileInfo targetTFile = getTFileByPath(targetPath);
        try {
            AliyunDriveRequest.FileMoveInfo query = new AliyunDriveRequest.FileMoveInfo(
                    getDefaultDriveId(), sourceTFile.getFileId(), targetTFile.getFileId()
            );
            AliyunDriveResponse.FileMoveInfo res = this.mAliyunDrive.fileMove(query).execute();
            if (!StringUtils.isEmpty(res.getCode())) {
                throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
            }
        } finally {
            clearCache(sourceTFile.getFileId());
            clearCache(sourceTFile.getParentFileId());
            clearCache(targetTFile.getFileId());
            clearCache(targetTFile.getParentFileId());
        }
    }

    public void remove(@Nullable AliyunDriveFileInfo tFile) {
        if (tFile == null) {
            return;
        }
        try {
            AliyunDriveRequest.FileMoveToTrashInfo query = new AliyunDriveRequest.FileMoveToTrashInfo(
                    getDefaultDriveId(), tFile.getFileId()
            );
            AliyunDriveResponse.FileMoveToTrashInfo res = this.mAliyunDrive.fileMoveToTrash(query).execute();
            if (!StringUtils.isEmpty(res.getCode())) {
                throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
            }
        } finally {
            clearCache(tFile.getFileId());
            clearCache(tFile.getParentFileId());
        }
    }

    public void remove(String path) {
        path = normalizingPath(path);
        AliyunDriveFileInfo tFile = getTFileByPath(path);
        remove(tFile);
    }

    public void createFolder(String path) {
        path = normalizingPath(path);
        PathInfo pathInfo = getPathInfo(path);
        AliyunDriveFileInfo parent = getTFileByPath(pathInfo.getParentPath());
        if (parent == null) {
            LOGGER.warn("创建目录失败，未发现父级目录：{}", pathInfo.getParentPath());
            return;
        }
        try {
            AliyunDriveRequest.FileCreateInfo query = new AliyunDriveRequest.FileCreateInfo(
                    getDefaultDriveId(), parent.getFileId(), pathInfo.getName(),
                    AliyunDriveEnum.Type.Folder, AliyunDriveEnum.CheckNameMode.Refuse
            );
            AliyunDriveResponse.FileCreateInfo res = this.mAliyunDrive.fileCreate(query).execute();
            try {
                if (!StringUtils.isEmpty(res.getCode())) {
                    throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
                }
            } finally {
                clearCache(res.getFileId());
            }
        } finally {
            clearCache(parent.getFileId());
        }
    }

    public AliyunDriveFileInfo getTFileByPath(String path) {
        path = normalizingPath(path);
        return getNodeIdByPath2(path);
    }

    public Response download(String path, JapHttpRequest request, long size) {
        AliyunDriveFileInfo file = getTFileByPath(path);
        String range = extractRangeHeader(request, size);
        String ifRange = extractIfRangeHeader(request);
        AliyunDriveResponse.FileGetDownloadUrlInfo res = fileGetDownloadUrlInfo(file.getFileId());
        if (res.isError()) {
            throw new WebdavException(new WebdavException(res.getCode(), res.getMessage()));
        }
        String url = res.getUrl().replaceAll("^https://", "http://");
        try {
            return this.mAliyunDrive.download(url, range, ifRange).execute();
        } catch (Throwable t) {
            throw new WebdavException(t);
        }
    }

    private synchronized AliyunDriveResponse.FileGetDownloadUrlInfo fileGetDownloadUrlInfo(String fileId) {
        AliyunDriveResponse.FileGetDownloadUrlInfo res;
        try {
            res = tFileDownloadUrlCache.get(fileId);
            Date expirationDate = res.getExpiration();
            if (expirationDate != null && new Date().after(expirationDate)) {
                tFileDownloadUrlCache.invalidate(fileId);
                res = tFileDownloadUrlCache.get(fileId);
            }
            return res;
        } catch (ExecutionException e) {
            res = new AliyunDriveResponse.FileGetDownloadUrlInfo();
            res.setCode(e.getMessage());
            res.setMessage(e.toString());
            return res;
        }
    }

    private synchronized AliyunDriveResponse.FileGetDownloadUrlInfo fileGetDownloadUrlInternal(String fileId) {
        AliyunDriveRequest.FileGetDownloadUrlInfo query = new AliyunDriveRequest.FileGetDownloadUrlInfo(
                getDefaultDriveId(), fileId
        );
        query.setExpireSec(AliyunDriveConstant.MAX_DOWNLOAD_URL_EXPIRE_TIME_SEC);
        AliyunDriveResponse.FileGetDownloadUrlInfo res = this.mAliyunDrive.fileGetDownloadUrl(query).execute();
        if (res.isError()) {
            if ("TooManyRequests".equals(res.getCode())) {
                try {
                    TimeUnit.SECONDS.sleep(6);
                } catch (InterruptedException e) {
                }
                res = this.mAliyunDrive.fileGetDownloadUrl(query).execute();
            }
        }
        if (res.isError()) {
            if ("TooManyRequests".equals(res.getCode())) {
                try {
                    TimeUnit.SECONDS.sleep(6);
                } catch (InterruptedException e) {
                }
                res = this.mAliyunDrive.fileGetDownloadUrl(query).execute();
            }
        }
        if (res.isError()) {
            LOGGER.error("fileGetDownloadUrlInternal code: " + res.getCode() + " message: " + res.getMessage());
            return null;
        }
        return res;
    }

    private String extractIfRangeHeader(JapHttpRequest request) {
        String ifRange = request.getHeader("if-range");
        if (ifRange == null) {
            return null;
        }
        return ifRange;
    }

    private String extractRangeHeader(JapHttpRequest request, long size) {
        String range = request.getHeader("range");
        if (range == null) {
            return null;
        }
        // 如果range最后 >= size， 则去掉
        String[] split = range.split("-");
        if (split.length == 2) {
            String end = split[1];
            if (Long.parseLong(end) >= size) {
                range = range.substring(0, range.lastIndexOf('-') + 1);
            }
        }
        return range;
    }

    private AliyunDriveFileInfo getNodeIdByPath2(String path) {
        if (StringUtils.isEmpty(path)) {
            path = rootPath;
        }
        if (path.equals(rootPath)) {
            return getRootTFile();
        }
        PathInfo pathInfo = getPathInfo(path);
        AliyunDriveFileInfo tFile = getTFileByPath(pathInfo.getParentPath());
        if (tFile == null) {
            return null;
        }
        return getNodeIdByParentId(tFile.getFileId(), pathInfo.getName());
    }

    public PathInfo getPathInfo(String path) {
        path = normalizingPath(path);
        if (path.equals(rootPath)) {
            PathInfo pathInfo = new PathInfo();
            pathInfo.setPath(path);
            pathInfo.setName(path);
            return pathInfo;
        }
        File file = new File(path);
        PathInfo pathInfo = new PathInfo();
        pathInfo.setPath(path);
        String parentPath = file.getParent();
        if (parentPath == null) {
            pathInfo.setParentPath("/");
        } else {
            pathInfo.setParentPath(parentPath.replace("\\", "/"));
        }
        pathInfo.setName(file.getName());
        return pathInfo;
    }

    private AliyunDriveFileInfo getRootTFile() {
        if (rootTFile == null) {
            rootTFile = new AliyunDriveFileInfo();
            rootTFile.setName("/");
            rootTFile.setFileId("root");
            rootTFile.setCreatedAt(new Date());
            rootTFile.setUpdatedAt(new Date());
            rootTFile.setType(AliyunDriveEnum.Type.Folder);
        }
        return rootTFile;
    }

    public AliyunDriveFileInfo getNodeIdByParentId(String parentId, String name) {
        Set<AliyunDriveFileInfo> tFiles = getTFiles(parentId);
        for (AliyunDriveFileInfo tFile : tFiles) {
            if (tFile.getName().equals(name)) {
                return tFile;
            }
        }
        return null;
    }

    private String normalizingPath(String path) {
        path = path.replace("\\", "/");
        path = path.replaceAll("//", "/");
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public void clearCache(String fileId) {
        if (StringUtils.isEmpty(fileId)) {
            return;
        }
        LOGGER.info("clearCache! {}", fileId);
        tFilesCache.invalidate(fileId);
    }

    public void clearCacheAll() {
        tFilesCache.invalidateAll();
    }

    private void loginAsync(String refreshToken) {
        loginAsync(refreshToken, null);
    }

    private void loginAsync(String refreshToken, final Runnable onFailureRunnable) {
        if (StringUtils.isEmpty(refreshToken)) {
            if (onFailureRunnable != null) {
                onFailureRunnable.run();
            }
            return;
        }
        AliyunDriveCall<AliyunDriveResponse.AccessTokenInfo> call;
        String accessToken = "";
        if (this.mAliyunDrive instanceof AliyunDriveWebApiImplV1) {
            AliyunDriveRequest.AccessTokenInfo query = new AliyunDriveRequest.AccessTokenInfo();
            query.setGrantType(AliyunDriveEnum.GrantType.RefreshToken);
            query.setRefreshToken(refreshToken);
            call = this.mAliyunDrive.getAccessToken(query);
        } else {
            String url = String.format(Locale.getDefault(), this.mAliyunDriveProperties.getAliyunAccessTokenUrl(), accessToken, refreshToken);
            call = this.mAliyunDrive.getAccessToken(url);
        }
        call.enqueue(new AliyunDriveCall.Callback<AliyunDriveResponse.AccessTokenInfo>() {
            @Override
            public void onResponse(Call call, Response response, AliyunDriveResponse.AccessTokenInfo res) {
                if (!res.isError()) {
                    AliyunDriveClientService.this.mAliyunDrive.setAccessTokenInfo(res);
                    AliyunDriveClientService.this.mAliyunDriveProperties.save(res);
                    LOGGER.info("登录成功! {}", JsonUtils.toJson(res));
                }
            }

            @Override
            public void onFailure(Call call, Throwable t, AliyunDriveResponse.AccessTokenInfo res) {
                if (onFailureRunnable != null) {
                    onFailureRunnable.run();
                }
            }
        });
    }

    @Override
    public AliyunDriveResponse.AccessTokenInfo acquireNewAccessToken(AliyunDriveResponse.AccessTokenInfo oldAccessTokenInfo) {
        if (this.mAliyunDrive instanceof AliyunDriveWebApiImplV1) {
            return acquireNewAccessTokenWebApi();
        }
        String refreshToken = this.mAliyunDriveProperties.getRefreshToken();
        String refreshTokenNext = this.mAliyunDriveProperties.getRefreshTokenNext();
        String accessToken = "";
        String url = "";
        AliyunDriveResponse.AccessTokenInfo res = null;
        if (StringUtils.isNotEmpty(refreshToken)) {
            url = String.format(Locale.getDefault(), this.mAliyunDriveProperties.getAliyunAccessTokenUrl(), accessToken, refreshToken);
            res = this.mAliyunDrive.getAccessToken(url).execute();
            if (!res.isError()) {
                AliyunDriveClientService.this.mAliyunDriveProperties.save(res);
                return res;
            }
        }
        if (StringUtils.isNotEmpty(refreshTokenNext)) {
            url = String.format(Locale.getDefault(), this.mAliyunDriveProperties.getAliyunAccessTokenUrl(), accessToken, refreshTokenNext);
            res = this.mAliyunDrive.getAccessToken(url).execute();
            if (!res.isError()) {
                AliyunDriveClientService.this.mAliyunDriveProperties.save(res);
                return res;
            }
        }
        return null;
    }

    private AliyunDriveResponse.AccessTokenInfo acquireNewAccessTokenWebApi() {

        if (this.mAliyunDrive instanceof AliyunDriveWebApiImplV1) {
        } else {
            throw new RuntimeException("Error: AliyunDrive class cast Error, expected: "
                    + AliyunDriveWebApiImplV1.class + " got: " + this.mAliyunDrive.getClass());
        }
        mAliyunDriveProperties.setAuthorization(null);
        AliyunDriveResponse.AccessTokenInfo res;
        AliyunDriveRequest.AccessTokenInfo query = new AliyunDriveRequest.AccessTokenInfo();
        query.setGrantType(AliyunDriveEnum.GrantType.RefreshToken);
        query.setRefreshToken(mAliyunDriveProperties.getRefreshToken());
        res = this.mAliyunDrive.getAccessToken(query).execute();
        if (res.isError()) {
            query.setRefreshToken(mAliyunDriveProperties.getRefreshTokenNext());
            res = this.mAliyunDrive.getAccessToken(query).execute();
        }
        if (res.isError()) {
            throw new IllegalStateException(res.getMessage() + "(" + res.getCode() + ")");
        }

        String accessToken = res.getAccessToken();
        String refreshToken = res.getRefreshToken();
        String userId = res.getUserId();
        if (StringUtils.isEmpty(res.getAccessToken()))
            throw new IllegalArgumentException("获取accessToken失败");
        if (StringUtils.isEmpty(res.getRefreshToken()))
            throw new IllegalArgumentException("获取refreshToken失败");
        if (StringUtils.isEmpty(userId))
            throw new IllegalArgumentException("获取userId失败");
        mAliyunDriveProperties.setUserId(userId);
        mAliyunDriveProperties.setAuthorization(accessToken);
        mAliyunDriveProperties.setRefreshToken(refreshToken);
        mAliyunDriveProperties.save();
        return res;
    }

    @Override
    public <T> T onAuthorizerEvent(String eventId, Object data, Class<T> resultCls) {
        if (StringUtils.isEmpty(eventId)) {
            return null;
        }
        switch (eventId) {
            case AliyunDriveWebConstant.Event.DEVICE_SESSION_SIGNATURE_INVALID: {
                if (this.mAliyunDrive instanceof AliyunDriveWebApiImplV1) {
                    AliyunDriveSessionManager mgr = new AliyunDriveSessionManager((AliyunDriveWebApiImplV1) this.mAliyunDrive, this.mAliyunDriveProperties);
                    mgr.updateSession();
                } else {
                    throw new RuntimeException("Error: AliyunDrive class cast Error, expected: "
                            + AliyunDriveWebApiImplV1.class + " got: " + this.mAliyunDrive.getClass());
                }
            }
            break;
            case AliyunDriveWebConstant.Event.USER_DEVICE_OFFLINE: {
                this.mAliyunDriveProperties.getSession().setNonce(0);
                this.mAliyunDriveProperties.getSession().setExpireTimeSec(0);
                this.mAliyunDriveProperties.save();
            }
            break;
            case AliyunDriveWebConstant.Event.ACQUIRE_DEVICE_ID: {
                return (T) this.mAliyunDriveProperties.getDeviceId();
            }
            case AliyunDriveWebConstant.Event.ACQUIRE_SESSION_SIGNATURE: {
                return (T) this.mAliyunDriveProperties.getSession().getSignature();
            }
        }
        return null;
    }

    public void setAccessTokenInvalidListener(Runnable listener) {
        this.mAliyunDrive.setAccessTokenInvalidListener(listener);
    }

    private LoadingCache<String, AliyunDriveResponse.UserSpaceInfo> tSpaceInfoCache = CacheBuilder.newBuilder()
            .initialCapacity(128)
            .maximumSize(10240)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(new CacheLoader<String, AliyunDriveResponse.UserSpaceInfo>() {
                @Override
                public AliyunDriveResponse.UserSpaceInfo load(String key) throws Exception {
                    return AliyunDriveClientService.this.mAliyunDrive.getUserSpaceInfo().execute();
                }
            });

    public long getQuotaAvailableBytes() {
        try {
            AliyunDriveResponse.UserSpaceInfo res = tSpaceInfoCache.get("");
            if (res.isError()) {
                tSpaceInfoCache.invalidate("");
            }
            return res.getTotalSize() - res.getUsedSize();
        } catch (ExecutionException e) {
            LOGGER.error("getQuotaAvailableBytes", e);
        }
        return -1;
    }

    public long getQuotaUsedBytes() {
        try {
            AliyunDriveResponse.UserSpaceInfo res = tSpaceInfoCache.get("");
            if (res.isError()) {
                tSpaceInfoCache.invalidate("");
            }
            return res.getUsedSize();
        } catch (ExecutionException e) {
            LOGGER.error("getQuotaAvailableBytes", e);
        }
        return -1;
    }

    public void onAccountChanged() {
        this.clearCacheAll();
        this.mUserDriveInfo = null;
        this.tSpaceInfoCache.invalidateAll();
    }
}

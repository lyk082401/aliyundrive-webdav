package com.github.zxbu.webdavteambition.store;

import com.github.zxbu.webdavteambition.model.FileType;
import com.github.zxbu.webdavteambition.model.PathInfo;
import com.github.zxbu.webdavteambition.model.result.TFile;
import com.google.common.net.HttpHeaders;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.Transaction;
import net.sf.webdav.exceptions.WebdavException;
import okhttp3.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public class AliYunDriverFileSystemStore implements IWebdavStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliYunDriverFileSystemStore.class);

    private static AliYunDriverClientService aliYunDriverClientService = AliYunDriverClientService.getInstance();

    private final Pattern inSharePatten = Pattern.compile(".*!(?<shareId>[a-zA-Z0-9]{11})(?>\\/.|:(?<password>[a-zA-Z0-9]{4})\\/.)");

    public AliYunDriverFileSystemStore(File file) {
    }

    public static void setBean(AliYunDriverClientService aliYunDriverClientService) {
        AliYunDriverFileSystemStore.aliYunDriverClientService = aliYunDriverClientService;
    }




    @Override
    public void destroy() {
        LOGGER.info("destroy");

    }

    @Override
    public ITransaction begin(Principal principal, HttpServletRequest req, HttpServletResponse resp) {
        LOGGER.debug("begin");
        return new Transaction(principal, req, resp);
    }

    @Override
    public void checkAuthentication(ITransaction transaction) {
        LOGGER.debug("checkAuthentication");
    }

    @Override
    public void commit(ITransaction transaction) {
        LOGGER.debug("commit");
    }

    @Override
    public void rollback(ITransaction transaction) {
        LOGGER.debug("rollback");

    }

    @Override
    public void createFolder(ITransaction transaction, String folderUri) {
        LOGGER.info("createFolder {}", folderUri);
        if (inSharePatten.matcher(folderUri).find()){
            throw new WebdavException("共享目录不可写");
        }
        aliYunDriverClientService.createFolder(folderUri);
    }

    @Override
    public void createResource(ITransaction transaction, String resourceUri) {
        LOGGER.info("createResource {}", resourceUri);
        if (inSharePatten.matcher(resourceUri).find()){
            throw new WebdavException("共享目录不可写");
        }
    }

    @Override
    public InputStream getResourceContent(ITransaction transaction, String resourceUri) {
        LOGGER.info("getResourceContent: {}", resourceUri);
        Enumeration<String> headerNames = transaction.getRequest().getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String s = headerNames.nextElement();
            LOGGER.debug("{} request: {} = {}",resourceUri,  s, transaction.getRequest().getHeader(s));
        }
        HttpServletResponse response = transaction.getResponse();
        long size = getResourceLength(transaction, resourceUri);
        Response downResponse = aliYunDriverClientService.download(resourceUri, transaction.getRequest(), size);
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(downResponse.body().contentLength()));
        for (String name : downResponse.headers().names()) {
            //Fix Winscp Invalid Content-Length in response
            if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                continue;
            }
            LOGGER.debug("{} downResponse: {} = {}", resourceUri, name, downResponse.header(name));
            response.addHeader(name, downResponse.header(name));
        }
        response.setStatus(downResponse.code());
        return downResponse.body().byteStream();

    }

    @Override
    public long setResourceContent(ITransaction transaction, String resourceUri, InputStream content, String contentType, String characterEncoding) {
        LOGGER.info("setResourceContent {}", resourceUri);
        if (inSharePatten.matcher(resourceUri).find()){
            throw new WebdavException("共享目录不可写");
        }
        HttpServletRequest request = transaction.getRequest();
        HttpServletResponse response = transaction.getResponse();

        long contentLength = request.getContentLength();
        if (contentLength < 0) {
            contentLength = Long.parseLong(StringUtils.defaultIfEmpty(request.getHeader("content-length"), "-1"));
            if (contentLength < 0) {
                contentLength = Long.parseLong(StringUtils.defaultIfEmpty(request.getHeader("X-Expected-Entity-Length"), "-1"));
            }
        }
        aliYunDriverClientService.uploadPre(resourceUri, contentLength, content);

        if (contentLength == 0) {
            String expect = request.getHeader("Expect");

            // 支持大文件上传
            if ("100-continue".equalsIgnoreCase(expect)) {
                try {
                    response.sendError(100, "Continue");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        }
        return contentLength;
    }

    @Override
    public String[] getChildrenNames(ITransaction transaction, String folderUri) {
        LOGGER.info("getChildrenNames: {}", folderUri);
        TFile tFile = aliYunDriverClientService.getTFileByPath(folderUri);
        if (tFile.getType().equals(FileType.file.name())) {
            return new String[0];
        }
        Set<TFile> tFileList = aliYunDriverClientService.getTFiles(tFile.getFile_id(), tFile.getShare_id(), tFile.getShare_password());
        return tFileList.stream().map(TFile::getName).toArray(String[]::new);
    }



    @Override
    public long getResourceLength(ITransaction transaction, String path) {
        LOGGER.info("getResourceLength: {}", path);
        TFile tFile = aliYunDriverClientService.getTFileByPath(path);
        if (tFile == null || tFile.getSize() == null) {
            return 384;
        }

        return tFile.getSize();
    }

    @Override
    public void removeObject(ITransaction transaction, String uri) {
        LOGGER.info("removeObject: {}", uri);
        if (inSharePatten.matcher(uri).find()){
            // 对于共享文件夹内的删除进行跳过，因为在删除共享目录时候用户使用 rm -r 'Test!Pfb5mXu2TLK' 之类的命令或者使用 GUI 删除都会递归删除子文件，如果这里报错就会导致本身的文件夹也无法删除
            LOGGER.info("removeObject skip: {}", uri);
            return;
        }
        aliYunDriverClientService.remove(uri);
    }

    @Override
    public boolean moveObject(ITransaction transaction, String destinationPath, String sourcePath) {
        LOGGER.info("moveObject, destinationPath={}, sourcePath={}", destinationPath, sourcePath);
        if (inSharePatten.matcher(destinationPath).find() || inSharePatten.matcher(sourcePath).find()){
            throw new WebdavException("共享目录不可写");
        }
        PathInfo destinationPathInfo = aliYunDriverClientService.getPathInfo(destinationPath);
        PathInfo sourcePathInfo = aliYunDriverClientService.getPathInfo(sourcePath);
        // 名字相同，说明是移动目录
        if (sourcePathInfo.getName().equals(destinationPathInfo.getName())) {
            aliYunDriverClientService.move(sourcePath, destinationPathInfo.getParentPath());
        } else {
            if (!destinationPathInfo.getParentPath().equals(sourcePathInfo.getParentPath())) {
                throw new WebdavException("不支持目录和名字同时修改");
            }
            // 名字不同，说明是修改名字。不考虑目录和名字同时修改的情况
            aliYunDriverClientService.rename(sourcePath, destinationPathInfo.getName());
        }
        return true;
    }

    @Override
    public StoredObject getStoredObject(ITransaction transaction, String uri) {
        LOGGER.info("getStoredObject: {}", uri);
        TFile tFile = aliYunDriverClientService.getTFileByPath(uri);
        if (tFile == null) {
            return null;
        }
        StoredObject so = new StoredObject();
        if ("folder".equalsIgnoreCase(tFile.getType())) {
            so.setFolder(true);
            so.setResourceLength(0);
        } else {
            so.setFolder(false);
            so.setResourceLength(tFile.getSize());
        }
        so.setCreationDate(tFile.getCreated_at());
        so.setLastModified(tFile.getUpdated_at());
        return so;
    }
}

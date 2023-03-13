package com.github.zxbu.webdavteambition.store;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import com.github.zxbu.webdavteambition.model.PathInfo;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.UncheckedExecutionException;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.Transaction;
import net.sf.webdav.exceptions.WebdavException;
import net.xdow.aliyundrive.bean.AliyunDriveEnum;
import net.xdow.aliyundrive.bean.AliyunDriveFileInfo;
import net.xdow.aliyundrive.exception.NotAuthenticatedException;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

public class AliyunDriveFileSystemStore implements IWebdavStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunDriveFileSystemStore.class);

    private AliyunDriveClientService mAliyunDriveClientService;

    public AliyunDriveFileSystemStore(Object[] args, File root) {
        this.mAliyunDriveClientService = (AliyunDriveClientService) args[0];
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy");
    }

    @Override
    public ITransaction begin(Principal principal, JapHttpRequest req, JapHttpResponse resp) {
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
        mAliyunDriveClientService.createFolder(folderUri);
    }

    @Override
    public void createResource(ITransaction transaction, String resourceUri) {
        LOGGER.info("createResource {}", resourceUri);
    }

    @Override
    public InputStream getResourceContent(ITransaction transaction, String resourceUri) {
        LOGGER.info("getResourceContent: {}", resourceUri);
        Enumeration<String> headerNames = transaction.getRequest().getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String s = headerNames.nextElement();
            LOGGER.debug("{} request: {} = {}", resourceUri, s, transaction.getRequest().getHeader(s));
        }
        JapHttpResponse response = transaction.getResponse();
        long size = getResourceLength(transaction, resourceUri);
        Response downResponse = mAliyunDriveClientService.download(resourceUri, transaction.getRequest(), size);
        LOGGER.debug("{} code = {}", resourceUri, downResponse.code());
        for (String name : downResponse.headers().names()) {
            //Fix Winscp Invalid Content-Length in response
            if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                continue;
            }
            LOGGER.debug("{} downResponse: {} = {}", resourceUri, name, downResponse.header(name));
            response.addHeader(name, downResponse.header(name));
        }
        response.setContentLengthLong(downResponse.body().contentLength());
        response.setStatus(downResponse.code());
        return downResponse.body().byteStream();
    }

    @Override
    public long setResourceContent(ITransaction transaction, String resourceUri, InputStream content, String contentType, String characterEncoding) {
        LOGGER.info("setResourceContent {}", resourceUri);
        JapHttpRequest request = transaction.getRequest();
        JapHttpResponse response = transaction.getResponse();

        long contentLength = -1;
        try {
            contentLength = request.getContentLength();
        } catch (IOException e) {
        }
        if (contentLength < 0) {
            contentLength = Long.parseLong(StringUtils.defaultIfEmpty(request.getHeader("content-length"), "-1"));
            if (contentLength < 0) {
                contentLength = Long.parseLong(StringUtils.defaultIfEmpty(request.getHeader("X-Expected-Entity-Length"), "-1"));
            }
        }
        mAliyunDriveClientService.uploadPre(resourceUri, contentLength, content, response);

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
        AliyunDriveFileInfo tFile = this.mAliyunDriveClientService.getTFileByPath(folderUri);
        if (tFile.getType() == AliyunDriveEnum.Type.File) {
            return new String[0];
        }
        try {
            Set<AliyunDriveFileInfo> tFileList = this.mAliyunDriveClientService.getTFiles(tFile.getFileId());
            List<String> nameList = new ArrayList<>();
            for (AliyunDriveFileInfo file : tFileList) {
                tFile = file;
                nameList.add(tFile.getName());
            }
            return nameList.toArray(new String[nameList.size()]);
        } catch (UncheckedExecutionException e) {
            if (e.getCause() instanceof NotAuthenticatedException) {
                return new String[]{"授权已失效" + e.getMessage()};

            }
        }
        return new String[0];
    }

    @Override
    public long getResourceLength(ITransaction transaction, String path) {
        return getResourceLength2(transaction, path);
    }

    private long getResourceLength2(ITransaction transaction, String path) {
        LOGGER.info("getResourceLength: {}", path);
        AliyunDriveFileInfo tFile = mAliyunDriveClientService.getTFileByPath(path);
        if (tFile == null || tFile.getSize() == null) {
            return 384;
        }

        return tFile.getSize();
    }

    @Override
    public void removeObject(ITransaction transaction, String uri) {
        LOGGER.info("removeObject: {}", uri);
        mAliyunDriveClientService.remove(uri);
    }

    @Override
    public boolean moveObject(ITransaction transaction, String destinationPath, String sourcePath) {
        LOGGER.info("moveObject, destinationPath={}, sourcePath={}", destinationPath, sourcePath);

        PathInfo destinationPathInfo = mAliyunDriveClientService.getPathInfo(destinationPath);
        PathInfo sourcePathInfo = mAliyunDriveClientService.getPathInfo(sourcePath);
        // 名字相同，说明是移动目录
        if (sourcePathInfo.getName().equals(destinationPathInfo.getName())) {
            mAliyunDriveClientService.move(sourcePath, destinationPathInfo.getParentPath());
        } else {
            if (!destinationPathInfo.getParentPath().equals(sourcePathInfo.getParentPath())) {
                throw new WebdavException("不支持目录和名字同时修改");
            }
            // 名字不同，说明是修改名字。不考虑目录和名字同时修改的情况
            mAliyunDriveClientService.rename(sourcePath, destinationPathInfo.getName());
        }
        return true;
    }

    @Override
    public StoredObject getStoredObject(ITransaction transaction, String uri) {
        if ("/favicon.ico".equals(uri)) {
            return null;
        }
        LOGGER.info("getStoredObject: {}", uri);
        AliyunDriveFileInfo tFile = mAliyunDriveClientService.getTFileByPath(uri);
        if (tFile != null) {
            StoredObject so = new StoredObject();
            so.setFolder(tFile.getType() == AliyunDriveEnum.Type.Folder);
            so.setResourceLength(getResourceLength2(transaction, uri));
            so.setCreationDate(tFile.getCreatedAt());
            so.setLastModified(tFile.getUpdatedAt());
            return so;
        }
        return null;
    }

    @Override
    public long getQuotaAvailableBytes(ITransaction transaction) {
        return mAliyunDriveClientService.getQuotaAvailableBytes();
    }

    @Override
    public long getQuotaUsedBytes(ITransaction transaction) {
        return mAliyunDriveClientService.getQuotaUsedBytes();
    }

    @Override
    public String getFooter(ITransaction transaction) {
        if (this.mAliyunDriveClientService.getAliyunDrive() instanceof AliyunDriveWebApiImplV1) {
            return "<br/><form action=\"/\">\n" +
                    "  <label for=\"refresh_token\">更换RefreshToken: </label>\n" +
                    "  <input type=\"text\" id=\"refresh_token\" name=\"refresh_token\">\n" +
                    "  <input type=\"submit\" value=\"提交\">\n" +
                    "</form> ";
        }
        return "";
    }
}

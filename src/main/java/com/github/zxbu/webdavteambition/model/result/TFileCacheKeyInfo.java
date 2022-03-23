package com.github.zxbu.webdavteambition.model.result;

public class TFileCacheKeyInfo {
    public String nodeId;
    public String shareId;
    public String sharePassword;

    public TFileCacheKeyInfo() {
    }

    public TFileCacheKeyInfo(String nodeId, String shareId, String sharePassword) {
        this.nodeId = nodeId;
        this.shareId = shareId;
        this.sharePassword = sharePassword;
    }
}

package com.github.zxbu.webdavteambition.model;

import lombok.Data;

@Data
public class UploadPreInfo {
    private String ccpParentId;
    private int chunkCount;
    private String contentType = "";
    private String driveId;
    private String name;
    private int size;
    private String type;
}

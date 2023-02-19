package com.github.zxbu.webdavteambition.model.result;

import lombok.Data;

@Data
public class CreateFileResult {
    private String ccpFileId;
    private String nodeId;
    private String name;
    private String kind;
}

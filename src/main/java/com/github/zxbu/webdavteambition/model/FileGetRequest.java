package com.github.zxbu.webdavteambition.model;

import lombok.Data;

@Data
public class FileGetRequest {
    private String drive_id;
    private String file_id;
}

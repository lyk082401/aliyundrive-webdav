package com.github.zxbu.webdavteambition.model;

import lombok.Data;

@Data
public class DownloadRequest {
    private String drive_id;
    private String file_id;
    private Integer expire_sec = 14400;
}

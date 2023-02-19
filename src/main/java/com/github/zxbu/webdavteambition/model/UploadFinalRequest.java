package com.github.zxbu.webdavteambition.model;

import lombok.Data;

@Data
public class UploadFinalRequest {
    private String drive_id;
    private String file_id;
    private String upload_id;
}

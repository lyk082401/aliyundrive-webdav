package com.github.zxbu.webdavteambition.model;

import lombok.Data;

import java.util.List;

@Data
public class RefreshUploadUrlRequest {
    private String drive_id;
    private List<UploadPreRequest.PartInfo> part_info_list;
    private String file_id;
    private String upload_id;
}

package com.github.zxbu.webdavteambition.model;

import lombok.Data;

import java.util.List;

@Data
public class UploadPreRequest {
    private String check_name_mode = "refuse";
    private String content_hash;
    private String content_hash_name = "none";
    private String drive_id;
    private String name;
    private String parent_file_id;
    private String proof_code;
    private String proof_version = "v1";
    private Long size;
    private List<PartInfo> part_info_list;
    private String type = "file";

    @Data
    public static class PartInfo {
        private Integer part_number;
        private String upload_url;
    }
}

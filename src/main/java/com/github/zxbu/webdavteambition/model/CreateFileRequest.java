package com.github.zxbu.webdavteambition.model;

import lombok.Data;

@Data
public class CreateFileRequest {
    private String check_name_mode = "refuse";
    private String drive_id;
    private String name;
    private String parent_file_id;
    private String type;
}

package com.github.zxbu.webdavteambition.model;

import lombok.Data;

@Data
public class RenameRequest {
    //refuse auto_rename
    private String check_name_mode = "refuse";
    private String drive_id;
    private String name;
    private String file_id;
}

package com.github.zxbu.webdavteambition.model;

import lombok.Data;

@Data
public class MoveRequest {
    private String drive_id;
    private String file_id;
    private String to_parent_file_id;
}

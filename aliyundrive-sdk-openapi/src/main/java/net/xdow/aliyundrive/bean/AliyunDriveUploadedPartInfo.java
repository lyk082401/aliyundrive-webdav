package net.xdow.aliyundrive.bean;

import lombok.Data;

@Data
public class AliyunDriveUploadedPartInfo {
    private String etag;
    private int part_number;
    private int part_size;
}

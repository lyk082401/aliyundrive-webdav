package net.xdow.aliyundrive.bean;

import lombok.Data;

@Data
public class AliyunDriveFilePartInfo {
    private long partNumber;
    private String uploadUrl;
    private int partSize = 1024;
}
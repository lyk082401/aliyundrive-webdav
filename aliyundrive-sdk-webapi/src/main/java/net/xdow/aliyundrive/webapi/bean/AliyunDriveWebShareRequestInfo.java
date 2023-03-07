package net.xdow.aliyundrive.webapi.bean;

import lombok.Data;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;

@Data
public class AliyunDriveWebShareRequestInfo extends AliyunDriveResponse.GenericMessageInfo {
    private String shareToken;
    private String expireTime;
    private int expiresIn = 7200;
}

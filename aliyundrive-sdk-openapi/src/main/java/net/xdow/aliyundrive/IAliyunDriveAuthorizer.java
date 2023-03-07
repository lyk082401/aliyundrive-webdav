package net.xdow.aliyundrive;

import net.xdow.aliyundrive.bean.AliyunDriveResponse;

public interface IAliyunDriveAuthorizer {
    AliyunDriveResponse.AccessTokenInfo acquireNewAccessToken(AliyunDriveResponse.AccessTokenInfo oldAccessTokenInfo);
    <T> T onAuthorizerEvent(String eventId, Object data, Class<T> resultCls);
}

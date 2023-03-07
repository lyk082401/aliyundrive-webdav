package net.xdow.aliyundrive.webapi.net;

import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.exception.NotAuthorizeException;
import net.xdow.aliyundrive.net.AliyunDriveCall;
import okhttp3.Call;

public class AliyunDriveWebCall<T extends AliyunDriveResponse.GenericMessageInfo> extends AliyunDriveCall<T> {

    public AliyunDriveWebCall(T mockResult) {
        super(mockResult);
    }

    public AliyunDriveWebCall(Call call) {
        super(call);
    }

    public AliyunDriveWebCall(Call call, Class<? extends AliyunDriveResponse.GenericMessageInfo> classOfT) {
        super(call, classOfT);
    }

    @Override
    public void checkAuthorize(T t) {
        super.checkAuthorize(t);
        if (!t.isError()) {
            return;
        }
        if (this.mDisableAuthorizeCheck) {
            return;
        }
        if ("InvalidParameter.RefreshToken".equals(t.getCode())) { //Web
            throw new NotAuthorizeException(t.getMessage() + "(" + t.getCode() + ")");
        }
    }
}

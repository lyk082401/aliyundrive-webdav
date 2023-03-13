package net.xdow.aliyundrive.net.interceptor;

import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.util.StringUtils;
import okhttp3.*;

import java.io.IOException;

public class AliyunDriveAuthenticateInterceptor implements Interceptor {

    public static final String HEADER_AUTHENTICATE_NAME = "x-authenticate!!";
    public static final String HEADER_AUTHENTICATE_VALUE = "1";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final IAccessTokenInfoGetter mIAccessTokenInfoGetter;

    public AliyunDriveAuthenticateInterceptor(IAccessTokenInfoGetter mIAccessTokenInfoGetter) {
        this.mIAccessTokenInfoGetter = mIAccessTokenInfoGetter;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (!HEADER_AUTHENTICATE_VALUE.equals(request.header(HEADER_AUTHENTICATE_NAME))) {
            return chain.proceed(request);
        }
        if (!checkAuthenticated()) {
            ResponseBody body = ResponseBody.create(JSON, "{\"code\":\"AccessTokenInvalid\",\"message\":\"access token is blank\",\"data\":null,\"headers\":null,\"pdsRequestId\":null,\"resultCode\":\"AccessTokenInvalid\",\"display_message\":null}");
            return new Response.Builder()
                    .addHeader("content-type", "application/json; charset=utf-8")
                    .body(body)
                    .code(401)
                    .message("access token is blank")
                    .protocol(Protocol.HTTP_1_0)
                    .request(chain.request())
                    .build();
        }
        return chain.proceed(request.newBuilder().removeHeader(HEADER_AUTHENTICATE_NAME).build());
    }

    public interface IAccessTokenInfoGetter {
        public AliyunDriveResponse.AccessTokenInfo getAccessTokenInfo();
    }

    private boolean checkAuthenticated() {
        AliyunDriveResponse.AccessTokenInfo info = this.mIAccessTokenInfoGetter.getAccessTokenInfo();
        if (info == null) {
            return false;
        }
        if (StringUtils.isEmpty(info.getAccessToken())) {
            return false;
        }
        if (StringUtils.isEmpty(info.getRefreshToken())) {
            return false;
        }
        return true;
    }
}

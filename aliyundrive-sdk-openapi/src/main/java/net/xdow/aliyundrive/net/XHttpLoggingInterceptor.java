package net.xdow.aliyundrive.net;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;

public class XHttpLoggingInterceptor implements Interceptor {

    public static final String SKIP_HEADER_NAME = "x-skip-http-log";
    public static final String SKIP_HEADER_VALUE = "1";

    final HttpLoggingInterceptor mLoggingInterceptor = new HttpLoggingInterceptor();

    public XHttpLoggingInterceptor() {
        mLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        try {
            Request request = chain.request();
            if (SKIP_HEADER_VALUE.equals(request.header(SKIP_HEADER_NAME))) {
                return chain.proceed(chain.request());
            }
            return mLoggingInterceptor.intercept(chain);
        } catch (Exception e) {
            System.out.println(e);
        }
        return chain.proceed(chain.request());
    }
}

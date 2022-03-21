package com.github.zxbu.webdavteambition.client;

import com.github.zxbu.webdavteambition.config.AliYunDriveProperties;
import com.github.zxbu.webdavteambition.model.ShareTokenRequest;
import com.github.zxbu.webdavteambition.model.result.ShareTokenResult;
import com.github.zxbu.webdavteambition.util.JsonUtil;
import net.sf.webdav.exceptions.WebdavException;
import okhttp3.*;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AliYunDriverClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliYunDriverClient.class);
    private static final String REFRESH_TOKEN_FILE_NAME = "refresh-token";
    private OkHttpClient okHttpClient;
    private AliYunDriveProperties aliYunDriveProperties;
    private Map<String, ShareTokenResult> shareTokenMapping = new HashMap<>();

    public AliYunDriverClient(AliYunDriveProperties aliYunDriveProperties) {

        OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                request = request.newBuilder()
                        .removeHeader("User-Agent")
                        .addHeader("User-Agent", aliYunDriveProperties.getAgent())
                        .removeHeader("authorization")
                        .addHeader("authorization", aliYunDriveProperties.getAuthorization())
                        .build();
                return chain.proceed(request);
            }
        }).authenticator(new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                if (response.code() == 401 && response.body() != null  && response.body().string().contains("AccessToken")) {
                    String refreshTokenResult;
                    try {
                        refreshTokenResult = post("https://api.aliyundrive.com/token/refresh", Collections.singletonMap("refresh_token", readRefreshToken()));
                    } catch (Exception e) {
                        // 如果置换token失败，先清空原token文件，再尝试一次
                        deleteRefreshTokenFile();
                        refreshTokenResult = post("https://api.aliyundrive.com/token/refresh", Collections.singletonMap("refresh_token", readRefreshToken()));
                    }
                    String accessToken = (String) JsonUtil.getJsonNodeValue(refreshTokenResult, "access_token");
                    String refreshToken = (String) JsonUtil.getJsonNodeValue(refreshTokenResult, "refresh_token");
                    if (StringUtils.isEmpty(accessToken)) throw new IllegalArgumentException("获取accessToken失败");
                    if (StringUtils.isEmpty(refreshToken)) throw new IllegalArgumentException("获取refreshToken失败");
                    aliYunDriveProperties.setAuthorization(accessToken);
                    writeRefreshToken(refreshToken);
                    return response.request().newBuilder()
                            .removeHeader("authorization")
                            .header("authorization", accessToken)
                            .build();
                }
                return null;
            }
        })
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .connectTimeout(1, TimeUnit.MINUTES)
                .build();
        this.okHttpClient = okHttpClient;
        this.aliYunDriveProperties = aliYunDriveProperties;
        init();
    }

    private void login() {
        // todo 暂不支持登录功能
    }

    public void init() {
        login();
        if (getDriveId() == null) {
            String personalJson = post("/user/get", Collections.emptyMap());
            String driveId = (String) JsonUtil.getJsonNodeValue(personalJson, "default_drive_id");
            aliYunDriveProperties.setDriveId(driveId);
        }
    }


    public String getDriveId() {
        return aliYunDriveProperties.getDriveId();
    }


    public Response download(String url, HttpServletRequest httpServletRequest, long size ) {
        Request.Builder builder = new Request.Builder().header("referer", "https://www.aliyundrive.com/");
        String range = httpServletRequest.getHeader("range");
        if (range != null) {
            // 如果range最后 >= size， 则去掉
            String[] split = range.split("-");
            if (split.length == 2) {
                String end = split[1];
                if (Long.parseLong(end) >= size) {
                    range = range.substring(0, range.lastIndexOf('-') + 1);
                }
            }
            builder.header("range", range);
        }

        String ifRange = httpServletRequest.getHeader("if-range");
        if (ifRange != null) {
            builder.header("if-range", ifRange);
        }


        Request request = builder.url(url).build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            return response;
        } catch (IOException e) {
            throw new WebdavException(e);
        }
    }

    public void upload(String url, byte[] bytes, final int offset, final int byteCount) {
        Request request = new Request.Builder()
                .put(RequestBody.create(MediaType.parse(""), bytes, offset, byteCount))
                .url(url).build();
        try (Response response = okHttpClient.newCall(request).execute()){
            LOGGER.info("upload {}, code {}", url, response.code());
            if (!response.isSuccessful()) {
                LOGGER.error("请求失败，url={}, code={}, body={}", url, response.code(), response.body().string());
                throw new WebdavException("请求失败：" + url);
            }
        } catch (IOException e) {
            throw new WebdavException(e);
        }
    }

    public String post(String url, Object body) {
        return post(url, body, null);
    }

    public String post(String url, Object body, String shareId) {
        String bodyAsJson = JsonUtil.toJson(body);
        Request.Builder requestBuilder = new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), bodyAsJson))
                .url(getTotalUrl(url));
        if (shareId != null){
            requestBuilder.header("X-Share-Token", readShareToken(shareId));
        }
        Request request = requestBuilder.build();
        try (Response response = okHttpClient.newCall(request).execute()){
            LOGGER.info("post {}, body {}, code {}", url, bodyAsJson, response.code());
            if (!response.isSuccessful()) {
                LOGGER.error("请求失败，url={}, code={}, body={}", url, response.code(), response.body().string());
                throw new WebdavException("请求失败：" + url);
            }
            return toString(response.body());
        } catch (IOException e) {
            throw new WebdavException(e);
        }
    }

    public String put(String url, Object body) {
        Request request = new Request.Builder()
                .put(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.toJson(body)))
                .url(getTotalUrl(url)).build();
        try (Response response = okHttpClient.newCall(request).execute()){
            LOGGER.info("put {}, code {}", url, response.code());
            if (!response.isSuccessful()) {
                LOGGER.error("请求失败，url={}, code={}, body={}", url, response.code(), response.body().string());
                throw new WebdavException("请求失败：" + url);
            }
            return toString(response.body());
        } catch (IOException e) {
            throw new WebdavException(e);
        }
    }

    public String get(String url, Map<String, String> params)  {
        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(getTotalUrl(url)).newBuilder();
            Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                String name = entry.getKey();
                String value = entry.getValue();
                urlBuilder.addQueryParameter(name, value);
            }

            Request request = new Request.Builder().get().url(urlBuilder.build()).build();
            try (Response response = okHttpClient.newCall(request).execute()){
                LOGGER.info("get {}, code {}", urlBuilder.build(), response.code());
                if (!response.isSuccessful()) {
                    throw new WebdavException("请求失败：" + urlBuilder.build().toString());
                }
                return toString(response.body());
            }

        } catch (Exception e) {
            throw new WebdavException(e);
        }

    }

    private String toString(ResponseBody responseBody) throws IOException {
        if (responseBody == null) {
            return null;
        }
        return responseBody.string();
    }

    private String getTotalUrl(String url) {
        if (url.startsWith("http")) {
            return url;
        }
        return aliYunDriveProperties.getUrl() + url;
    }

    public synchronized String readShareToken(String shareId){
        ShareTokenResult result = shareTokenMapping.get(shareId);
        if (result != null && result.getExpire_time().after(new Date())){
            return result.getShare_token();
        }
        ShareTokenRequest request = new ShareTokenRequest();
        request.setShare_id(shareId);
        String resultString = post("/share_link/get_share_token", request);
        result = JsonUtil.readValue(resultString, ShareTokenResult.class);
        shareTokenMapping.put(shareId, result);
        return result.getShare_token();
    }

    private void deleteRefreshTokenFile() {
        new File(aliYunDriveProperties.getWorkDir(), REFRESH_TOKEN_FILE_NAME).delete();
    }

    private String readRefreshToken() {
        File refreshTokenFile = new File(aliYunDriveProperties.getWorkDir(), REFRESH_TOKEN_FILE_NAME);

        if (!refreshTokenFile.exists()) {
            refreshTokenFile.getParentFile().mkdirs();
            try {
                FileUtils.write(refreshTokenFile, "", StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.error("Error: touch refreshTokenFile failed", e);
            }
        }
        try {
            byte[] bytes = FileUtils.readFileToByteArray(refreshTokenFile);
            if (bytes.length != 0) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LOGGER.warn("读取refreshToken文件 {} 失败: ", refreshTokenFile.getAbsolutePath(), e);
        }
        writeRefreshToken(aliYunDriveProperties.getRefreshToken());
        return aliYunDriveProperties.getRefreshToken();
    }

    private void writeRefreshToken(String newRefreshToken) {
        File refreshTokenFile = new File(aliYunDriveProperties.getWorkDir(), REFRESH_TOKEN_FILE_NAME);
        try {
            FileUtils.write(refreshTokenFile, newRefreshToken, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("写入refreshToken文件 {} 失败: ", refreshTokenFile.getAbsolutePath(), e);
        }
        aliYunDriveProperties.setRefreshToken(newRefreshToken);
    }
}

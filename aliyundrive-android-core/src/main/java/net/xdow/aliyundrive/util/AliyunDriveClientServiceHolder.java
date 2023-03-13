package net.xdow.aliyundrive.util;

import android.content.Context;
import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import net.xdow.aliyundrive.R;
import net.xdow.aliyundrive.event.AliyunDriveAccessTokenInvalidEvent;
import net.xdow.aliyundrive.impl.AliyunDriveOpenApiImplV1;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.greenrobot.eventbus.EventBus;

import java.io.File;

public class AliyunDriveClientServiceHolder {
    private static AliyunDriveClientService sAliyunDriveClientService;
    public static AliyunDriveClientService getInstance() {
        if (sAliyunDriveClientService == null) {
            synchronized (AliyunDriveClientServiceHolder.class) {
                if (sAliyunDriveClientService == null) {
                    ContextHandler.Context webContext = WebAppContext.getCurrentContext();
                    Context context = (Context) webContext.getAttribute("org.mortbay.ijetty.context");
                    String refreshToken = String.valueOf(webContext.getAttribute(context.getString(R.string.config_refresh_token)));
                    AliyunDriveProperties properties = AliyunDriveProperties.load(context.getFilesDir().getAbsolutePath() + File.separator);
                    properties.setAuthorization(null);
                    if (!refreshToken.equals(properties.getRefreshTokenNext())) {
                        properties.setRefreshToken(refreshToken);
                    }
                    properties.setRefreshTokenNext(refreshToken);
                    properties.setDeviceId(String.valueOf(webContext.getAttribute(context.getString(R.string.config_device_id))));
                    properties.setWorkDir(context.getFilesDir().getAbsolutePath() + File.separator);
                    properties.save();
                    boolean useAliyunDriveOpenApi = Boolean.parseBoolean(String.valueOf(webContext.getAttribute(context.getString(R.string.config_use_aliyun_drive_openapi))));
                    if (useAliyunDriveOpenApi) {
                        sAliyunDriveClientService = new AliyunDriveClientService(AliyunDriveOpenApiImplV1.class, properties);
                    } else {
                        sAliyunDriveClientService = new AliyunDriveClientService(AliyunDriveWebApiImplV1.class, properties);
                    }
                    sAliyunDriveClientService.setAccessTokenInvalidListener(() -> EventBus.getDefault().post(new AliyunDriveAccessTokenInvalidEvent()));
                }
            }
        }
        return sAliyunDriveClientService;
    }

    public static void onShutdown() {
        synchronized (AliyunDriveClientServiceHolder.class) {
            sAliyunDriveClientService = null;
        }
    }
}

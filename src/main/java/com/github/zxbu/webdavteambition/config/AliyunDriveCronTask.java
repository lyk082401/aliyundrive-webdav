package com.github.zxbu.webdavteambition.config;

import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import com.github.zxbu.webdavteambition.util.AliyunDriveClientServiceHolder;
import net.xdow.aliyundrive.bean.AliyunDriveFileInfo;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AliyunDriveCronTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(AliyunDriveCronTask.class);

    @Autowired
    private AliyunDriveClientServiceHolder mAliyunDriveClientServiceHolder;

    /**
     * 每隔10分钟请求一下接口，保证token不过期
     */
    @Scheduled(initialDelay = 30 * 1000, fixedDelay = 10 * 60 * 1000)
    public void refreshToken() {
        AliyunDriveClientService service = this.mAliyunDriveClientServiceHolder.getAliyunDriveClientService();
        if (service.getAliyunDrive() instanceof AliyunDriveWebApiImplV1) {

        } else {
            return;
        }
        try {
            LOGGER.info("定时刷新 Refresh Token ↓↓↓↓↓");
            AliyunDriveFileInfo root = service.getTFileByPath("/");
            service.getTFileListCached(root.getFileId());
        } catch (Throwable e) {
            LOGGER.error("", e);
        } finally {
            LOGGER.info("定时刷新 Refresh Token ↑↑↑↑↑");
        }
    }
}

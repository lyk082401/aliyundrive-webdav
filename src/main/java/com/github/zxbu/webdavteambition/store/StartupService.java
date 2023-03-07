package com.github.zxbu.webdavteambition.store;

import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.manager.AliyunDriveSessionManager;
import com.github.zxbu.webdavteambition.util.AliyunDriveClientServiceHolder;
import net.xdow.aliyundrive.IAliyunDrive;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupService {

    private AliyunDriveSessionManager mAliyunDriveSessionManager;

    @Autowired
    private AliyunDriveClientServiceHolder mAliyunDriveClientServiceHolder;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent() {
        startAliyunDriveSessionManager();
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosedEvent(ContextClosedEvent contextClosedEvent) {
        stopAliyunDriveSessionManager();
    }

    private void startAliyunDriveSessionManager(){
        AliyunDriveClientService service = this.mAliyunDriveClientServiceHolder.getAliyunDriveClientService();
        AliyunDriveProperties properties = service.getProperties();
        IAliyunDrive aliyunDrive = service.getAliyunDrive();
        if (aliyunDrive instanceof AliyunDriveWebApiImplV1) {
        } else {
            return;
        }
        AliyunDriveSessionManager mgr = this.mAliyunDriveSessionManager;
        if (mgr != null) {
            mgr.stop();
        }
        mgr = new AliyunDriveSessionManager((AliyunDriveWebApiImplV1) aliyunDrive, properties);
        mAliyunDriveSessionManager = mgr;
        mgr.start();
    }

    private void stopAliyunDriveSessionManager(){
        AliyunDriveSessionManager mgr = mAliyunDriveSessionManager;
        if (mgr != null) {
            mgr.stop();
            mAliyunDriveSessionManager = null;
        }
    }
}

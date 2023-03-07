package com.github.zxbu.webdavteambition.util;

import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.config.AliyunDrivePropertiesSpring;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import net.xdow.aliyundrive.impl.AliyunDriveOpenApiImplV1;
import net.xdow.aliyundrive.webapi.impl.AliyunDriveWebApiImplV1;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AliyunDriveClientServiceHolder implements InitializingBean {

    @Autowired
    private AliyunDrivePropertiesSpring mAliyunDriveProperties;
    private AliyunDriveClientService mAliyunDriveClientService;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.mAliyunDriveProperties.getDriver() == AliyunDriveProperties.Driver.WebApi) {
            this.mAliyunDriveClientService = new AliyunDriveClientService(AliyunDriveWebApiImplV1.class, mAliyunDriveProperties);
        } else {
            this.mAliyunDriveClientService = new AliyunDriveClientService(AliyunDriveOpenApiImplV1.class, mAliyunDriveProperties);
        }
    }

    public AliyunDriveClientService getAliyunDriveClientService() {
        return this.mAliyunDriveClientService;
    }
}

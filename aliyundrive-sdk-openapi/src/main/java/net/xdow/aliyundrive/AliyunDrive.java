package net.xdow.aliyundrive;

import net.xdow.aliyundrive.impl.AliyunDriveOpenApiImplV1;

public class AliyunDrive {

    public static IAliyunDrive newAliyunDrive() {
        return new AliyunDriveOpenApiImplV1();
    }

    public static IAliyunDrive newAliyunDrive(Class<? extends IAliyunDrive> cls) {
        try {
            return cls.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}

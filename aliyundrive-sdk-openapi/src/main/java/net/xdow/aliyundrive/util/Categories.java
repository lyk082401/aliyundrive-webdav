package net.xdow.aliyundrive.util;

import net.xdow.aliyundrive.bean.AliyunDriveEnum;

public class Categories {
    public static String toString(AliyunDriveEnum.Category... categories) {
        return JsonUtils.toJson(categories).replaceAll("\\[|\\]|\\\"", "");
    }
}

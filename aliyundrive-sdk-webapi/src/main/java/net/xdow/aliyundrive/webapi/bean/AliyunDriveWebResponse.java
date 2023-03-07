package net.xdow.aliyundrive.webapi.bean;

import lombok.Data;
import lombok.Getter;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;

public class AliyunDriveWebResponse {

    @Data
    public static class ShareTokenInfo extends AliyunDriveWebShareRequestInfo {
    }

    @Data
    public static class ShareSaveInfo extends AliyunDriveResponse.GenericMessageInfo {
        private String driveId;
        private String fileId;
    }

    public static class UserSpaceInfo extends AliyunDriveResponse.UserSpaceInfo {
        private long driveUsedSize;
        private long driveTotalSize;
        @Getter
        private long defaultDriveUsedSize;
        @Getter
        private long albumDriveUsedSize;
        @Getter
        private long shareAlbumDriveUsedSize;
        @Getter
        private long noteDriveUsedSize;
        @Getter
        private long sboxDriveUsedSize;

        public long getTotalSize() {
            return this.driveTotalSize;
        }

        public long getUsedSize() {
            return this.driveUsedSize;
        }
    }
}

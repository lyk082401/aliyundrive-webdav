package net.xdow.aliyundrive.bean;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Date;

@Data
public class AliyunDriveFileInfo extends AliyunDriveResponse.GenericMessageInfo {
    private String driveId;
    private String fileId;
    private String parentFileId;
    private String name;
    private Long size;
    private String fileExtension;
    private String contentHash;
    private AliyunDriveEnum.Category category;
    private AliyunDriveEnum.Type type;
    private String thumbnail;
    private String url;
    @Deprecated
    private String downloadUrl;
    private Date createdAt;
    private Date updatedAt;

    /**
     * WebApi Only
     */
    @SerializedName("video_media_metadata")
    private AliyunDriveMediaMetaData videoMediaMetaData;


    public boolean isDirectory() {
        return type == AliyunDriveEnum.Type.Folder;
    }

    public boolean isFile() {
        return type == AliyunDriveEnum.Type.File;
    }
}

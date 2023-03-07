package net.xdow.aliyundrive.bean;

import com.google.gson.annotations.SerializedName;

public class AliyunDriveEnum {

    public enum GrantType {
        @SerializedName("authorization_code")
        AuthorizationCode,
        @SerializedName("refresh_token")
        RefreshToken,
    }

    public enum OrderDirection {
        @SerializedName("DESC")
        Desc,
        @SerializedName("ASE")
        Asc,
    }

    public enum Category {
        @SerializedName("video")
        Video,
        @SerializedName("doc")
        Doc,
        @SerializedName("audio")
        Audio,
        @SerializedName("zip")
        Zip,
        @SerializedName("image")
        Image,
        @SerializedName("others")
        Others,
    }

    public enum Type {
        @SerializedName("all")
        All,
        @SerializedName("file")
        File,
        @SerializedName("folder")
        Folder,
    }

    public enum CheckNameMode {
        @SerializedName("auto_rename")
        AutoRename,
        @SerializedName("refuse")
        Refuse,
        @SerializedName("ignore")
        Ignore,
    }

    public enum OrderBy {
        @SerializedName("created_at")
        CreatedAt,
        @SerializedName("updated_at")
        UpdatedAt,
        @SerializedName("name")
        Name,
        @SerializedName("size")
        Size,
    }

    public enum QrCodeState {
        @SerializedName("WaitLogin")
        WaitLogin,
        @SerializedName("ScanSuccess")
        ScanSuccess,
        @SerializedName("LoginSuccess")
        LoginSuccess,
        @SerializedName("QRCodeExpired")
        QrCodeExpired,
    }
}

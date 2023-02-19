package com.github.zxbu.webdavteambition.model.result;

import com.github.zxbu.webdavteambition.model.VideoMediaMetaDataInfo;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.Date;
@Data
public class TFile {
    private Date created_at;
    private String domain_id;
    private String drive_id;
    private String encrypt_mode;
    private String file_id;
    private Boolean hidden;
    private String name;
    private String file_name;
    private String parent_file_id;
    private String starred;
    private String status;
    private String type;
    private Date updated_at;
    private String url;
    private Long size;
    private String download_url;
    private String share_id;
    private String share_password;
    private String origin_file_id;
    private String thumbnail;

    @Nullable
    private VideoMediaMetaDataInfo video_media_metadata;
}

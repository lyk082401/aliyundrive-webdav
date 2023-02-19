package com.github.zxbu.webdavteambition.model;

import lombok.Data;

@Data
public class FileGetRequest {

    private String drive_id;
    private String fields = "*";
    private String file_id;
    private String share_id;
    private String image_thumbnail_process = "image/resize,w_400/format,jpeg";
    private String image_url_process = "image/resize,w_1920/format,jpeg";
    private String video_thumbnail_process = "video/snapshot,t_1000,f_jpg,ar_auto,w_300";

    public void set_thumbnail_time_ms(long time_ms) {
        setVideo_thumbnail_process("video/snapshot,t_" + time_ms + ",f_jpg,ar_auto,w_300");
    }
}

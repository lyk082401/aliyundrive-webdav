package com.github.zxbu.webdavteambition.model;

import lombok.Data;

@Data
public class FileListRequest extends Page{
    private String drive_id;
    private Boolean all = false;
    private String fields = "*";
    private String image_thumbnail_process = "image/resize,w_400/format,jpeg";
    private String image_url_process = "image/resize,w_1920/format,jpeg";
    private String parent_file_id;
    private String video_thumbnail_process = "video/snapshot,t_0,f_jpg,ar_auto,w_300";
}

package com.github.zxbu.webdavteambition.model;

public class FileGetRequest {

    private String drive_id;
    private String fields = "*";
    private String file_id;
    private String share_id;
    private String image_thumbnail_process = "image/resize,w_400/format,jpeg";
    private String image_url_process = "image/resize,w_1920/format,jpeg";
    private String video_thumbnail_process = "video/snapshot,t_1000,f_jpg,ar_auto,w_300";

    public String getDrive_id() {
        return drive_id;
    }

    public void setDrive_id(String drive_id) {
        this.drive_id = drive_id;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }

    public String getFile_id() {
        return file_id;
    }

    public void setFile_id(String file_id) {
        this.file_id = file_id;
    }

    public String getShare_id() {
        return share_id;
    }

    public void setShare_id(String share_id) {
        this.share_id = share_id;
    }

    public String getImage_thumbnail_process() {
        return image_thumbnail_process;
    }

    public void setImage_thumbnail_process(String image_thumbnail_process) {
        this.image_thumbnail_process = image_thumbnail_process;
    }

    public String getImage_url_process() {
        return image_url_process;
    }

    public void setImage_url_process(String image_url_process) {
        this.image_url_process = image_url_process;
    }

    public String getVideo_thumbnail_process() {
        return video_thumbnail_process;
    }

    public void setVideo_thumbnail_process(String video_thumbnail_process) {
        this.video_thumbnail_process = video_thumbnail_process;
    }

    public void set_thumbnail_time_ms(long time_ms) {
        setVideo_thumbnail_process("video/snapshot,t_" + time_ms + ",f_jpg,ar_auto,w_300");
    }
}

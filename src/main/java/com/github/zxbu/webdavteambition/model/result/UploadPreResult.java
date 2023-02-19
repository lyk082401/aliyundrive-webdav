package com.github.zxbu.webdavteambition.model.result;

import com.github.zxbu.webdavteambition.model.UploadPreRequest;
import lombok.Data;

import java.util.List;
@Data
public class UploadPreResult {
    private String file_id;
    private String file_name;
    private String location;
    private Boolean rapid_upload;
    private String  type;
    private String upload_id;
    private List<UploadPreRequest.PartInfo> part_info_list;
}

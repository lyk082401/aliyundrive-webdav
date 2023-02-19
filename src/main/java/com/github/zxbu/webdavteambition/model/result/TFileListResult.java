package com.github.zxbu.webdavteambition.model.result;

import lombok.Data;

import java.util.List;
@Data
public class TFileListResult<T> {
    private List<T> items;
    private String next_marker;
}

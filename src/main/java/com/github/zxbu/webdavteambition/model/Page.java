package com.github.zxbu.webdavteambition.model;

import lombok.Data;

@Data
public class Page {
    private String marker;
    private int limit;
    private String order_by;
    private String order_direction;
}

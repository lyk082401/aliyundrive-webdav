package com.github.zxbu.webdavteambition.filter;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;

public interface IErrorFilter {
    void doFilterCall(JapHttpRequest req, JapHttpResponse res, IErrorWrapperResponse wrapperResponse);
}

package net.sf.webdav;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;

import java.security.Principal;

public interface ITransaction {

    Principal getPrincipal();

    JapHttpRequest getRequest();

    JapHttpResponse getResponse();
}

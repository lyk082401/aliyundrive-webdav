package com.fujieid.jap.http.adapter.jakarta;

import com.fujieid.jap.http.JapHttpCookie;
import jakarta.servlet.http.Cookie;


/**
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 */
public class JakartaCookieAdapter extends JapHttpCookie {
    public JakartaCookieAdapter(Object cookie) {
        super(cookie);
        Cookie jakartaCookie = (Cookie) cookie;
        super.setDomain(jakartaCookie.getDomain());
        super.setPath(jakartaCookie.getPath());
        super.setName(jakartaCookie.getName());
        super.setValue(jakartaCookie.getValue());
        super.setMaxAge(jakartaCookie.getMaxAge());
        super.setSecure(jakartaCookie.getSecure());
        super.setHttpOnly(jakartaCookie.isHttpOnly());
        super.setComment(jakartaCookie.getComment());
        super.setVersion(jakartaCookie.getVersion());
    }
}

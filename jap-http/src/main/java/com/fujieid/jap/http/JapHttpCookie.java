package com.fujieid.jap.http;

/**
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0.0
 * @since 1.0.5
 */
public class JapHttpCookie {

    private String name;
    private String value;
    private String comment;
    private int version;
    private String domain;
    private String path = "/";
    private int maxAge = -1;
    private boolean secure;
    private boolean httpOnly;

    public JapHttpCookie(Object cookie) {
    }

    public JapHttpCookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public JapHttpCookie setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public JapHttpCookie setValue(String value) {
        this.value = value;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public JapHttpCookie setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getPath() {
        return path;
    }

    public JapHttpCookie setPath(String path) {
        this.path = path;
        return this;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public JapHttpCookie setMaxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public boolean isSecure() {
        return secure;
    }

    public JapHttpCookie setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public JapHttpCookie setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

}

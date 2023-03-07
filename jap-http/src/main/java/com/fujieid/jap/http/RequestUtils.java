package com.fujieid.jap.http;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * http servlet request util
 *
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0.0
 * @since 1.0.1
 */
public class RequestUtils {

    /**
     * Get the url parameter value of the request through {@code request.getParameter(paramName)}
     *
     * @param paramName parameter name
     * @param request   current HTTP request
     * @return string
     */
    public static String getParam(String paramName, JapHttpRequest request) {
        if (null == request) {
            return null;
        }
        return request.getParameter(paramName);
    }

    /**
     * Get request header
     *
     * @param headerName request header name
     * @param request    current HTTP request
     * @return string
     */
    public static String getHeader(String headerName, JapHttpRequest request) {
        if (null == request) {
            return "";
        }
        return request.getHeader(headerName);
    }

    /**
     * Get the referer of the current HTTP request
     *
     * @param request current HTTP request
     * @return string
     */
    public static String getReferer(JapHttpRequest request) {
        return getHeader("Referer", request);
    }

    /**
     * Get subdomain name
     *
     * @param request current HTTP request
     * @return string
     */
    public static String getFullDomainName(JapHttpRequest request) {
        StringBuffer url = request.getRequestUrl();
        return url.delete(url.length() - request.getRequestUri().length(), url.length()).toString();
    }

    /**
     * Get the User-Agent of the current HTTP request
     *
     * @param request current HTTP request
     * @return string
     */
    public static String getUa(JapHttpRequest request) {
        return getHeader("User-Agent", request);
    }

    /**
     * Get the IP of the current HTTP request
     *
     * @param request current HTTP request
     * @return string
     */
    public static String getIp(JapHttpRequest request) {
        if (null == request) {
            return "";
        }
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        String ip;
        for (String header : headers) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                return getMultistageReverseProxyIp(ip);
            }
        }
        ip = request.getRemoteAddr();
        return getMultistageReverseProxyIp(ip);
    }

    /**
     * Obtain the first non-unknown ip address from the multi-level reverse proxy
     *
     * @param ip IP
     * @return The first non-unknown ip address
     */
    private static String getMultistageReverseProxyIp(String ip) {
        if (ip != null && ip.indexOf(",") > 0) {
            final String[] ips = ip.trim().split(",");
            for (String subIp : ips) {
                if (isValidIp(subIp)) {
                    ip = subIp;
                    break;
                }
            }
        }
        return ip;
    }

    /**
     * Verify ip legitimacy
     *
     * @param ip ip
     * @return boolean
     */
    private static boolean isValidIp(String ip) {
        return isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip);
    }

    /**
     * Get the value of the cookie
     *
     * @param request current HTTP request
     * @param name    cookie name
     * @return String
     */
    public static String getCookieVal(JapHttpRequest request, String name) {
        JapHttpCookie cookie = getCookie(request, name);
        return cookie != null ? cookie.getValue() : null;
    }

    private static boolean isNotEmpty(String s) {
        return s != null && s.trim().length() != 0;
    }

    /**
     * Get the request url
     *
     * @param encode  Whether to encode url
     * @param request current HTTP request
     * @return string
     */
    public static String getRequestUrl(boolean encode, JapHttpRequest request) {
        if (null == request) {
            return "";
        }
        String currentUrl = request.getRequestUrl().toString();
        String queryString = request.getQueryString();
        if (isNotEmpty(queryString)) {
            currentUrl = currentUrl + "?" + queryString;
        }

        if (encode) {
            String result = "";
            try {
                result = URLEncoder.encode(currentUrl, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                //ignore
            }
            return result;
        }

        return currentUrl;
    }

    /**
     * Get cookie
     *
     * @param request current HTTP request
     * @param name    cookie name
     * @return Cookie
     */
    public static JapHttpCookie getCookie(JapHttpRequest request, String name) {
        JapHttpCookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (JapHttpCookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * Get all the cookies, and use the cookie name as the key to form a map
     *
     * @param request current HTTP request
     * @return Map
     */
    public static Map<String, JapHttpCookie> getCookieMap(JapHttpRequest request) {
        final JapHttpCookie[] cookies = request.getCookies();
        if (null == cookies || cookies.length == 0) {
            return new HashMap<>(0);
        }
        Map<String, JapHttpCookie> map = new HashMap<String, JapHttpCookie>();
        for (JapHttpCookie cookie : cookies) {
            map.put(cookie.getName(), cookie);
        }
        return map;
    }

    /**
     * Set cookie
     *
     * @param response current HTTP response
     * @param name     cookie name
     * @param value    cookie value
     * @param maxAge   maxAge
     * @param path     path
     * @param domain   domain
     */
    public static void setCookie(JapHttpResponse response, String name, String value, int maxAge, String path, String domain) {
        JapHttpCookie cookie = new JapHttpCookie(name, value);
        cookie.setPath(path);
        if (null != domain) {
            cookie.setDomain(domain);
        }
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(false);
        addCookie(response, cookie);
    }

    private static void addCookie(JapHttpResponse response, JapHttpCookie cookie) {
        if (null == cookie) {
            response.addCookie(null, null, null, null, 0, false, false);
            return;
        }
        response.addCookie(cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain(),
                cookie.getMaxAge(), cookie.isSecure(), cookie.isHttpOnly()
        );
    }
}

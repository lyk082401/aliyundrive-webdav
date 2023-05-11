# jap-http-adapter

#### 介绍
适配  jakarta.servlet-api、blade-mvc 的 http 接口，包括：request、response、cookie、session

- http 适配器：https://gitee.com/fujieid/jap-http-adapter
- http 接口：https://gitee.com/fujieid/jap-http

## 快速开始

### 适配 `jakarta.servlet`

1. 依次引入下方依赖
```xml
<dependency>
    <groupId>com.fujieid.jap.http.adapter</groupId>
    <artifactId>jap-http-jakarta-adapter</artifactId>
    <version>1.0.5</version>
</dependency>
```
2. 使用适配器重新构造

```java
// 适配 HttpServletRequest
new JakartaRequestAdapter(HttpServletRequest);
// 适配 Cookie
new JakartaCookieAdapter(Cookie);
// 适配 HttpSession
new JakartaSessionAdapter(HttpSession);
// 适配 HttpServletResponse
new JakartaResponseAdapter(HttpServletResponse);
```

### 适配 `blade` 框架

1. 依次引入下方依赖

```xml
<dependency>
    <groupId>com.fujieid.jap.http.adapter</groupId>
    <artifactId>jap-http-blade-adapter</artifactId>
    <version>1.0.5</version>
</dependency>
```
2. 使用适配器重新构造

```java
// 适配 HttpRequest
new BladeRequestAdapter(HttpRequest);
// 适配 Cookie
new BladeCookieAdapter(Cookie);
// 适配 Session
new BladeSessionAdapter(Session);
// 适配 HttpResponse
new BladeResponseAdapter(HttpResponse);
```

# jap-http

## 介绍

抽象 javax.servlet.http 包下的类，包括：request、response、cookie、session，用来适配第三方框架，比如：blade、jakarta等

- http 接口：https://gitee.com/fujieid/jap-http
- http 适配器：https://gitee.com/fujieid/jap-http-adapter

## 为什么要开发这个东西？

在开发 [jap](https://gitee.com/fujieid/jap) 的时候，碰到一个问题：在 blade 框架中集成 jap 时，因为 blade 中是自实现的一套 web 组件，没有 `HttpServletRequest` 等相关接口，那么也就意味着在 blade 中，无法使用 jap。

这叔能忍，婶不能忍。jap 的设计理念中重要的一条就是：**复杂的东西接口化**，就类似 `simple-http` 的使用，开发者想使用何种工具/框架，我们不干预。

因此，对于 `jakarta.servlet` 依赖中的 `javax.servlet.http` 包下的内容，全部重构，提取出一套标准的接口，这样的话，就可以适配任何框架，nice~！

## 快速开始

1. 引入依赖
```xml
<dependency>
    <groupId>com.fujieid.jap.http</groupId>
    <artifactId>jap-http</artifactId>
    <version>1.0.0</version>
</dependency>
```
2. 引入需要适配的第三方框架，此处以 `jakarta.servlet` 为例
```xml
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>4.0.4</version>
</dependency>
```
3. 实现 `jap-http` 的接口，以 `request` 为例
```java
public class JakartaRequestAdapter implements JapHttpRequest {

    private final HttpServletRequest request;

    public JakartaRequestAdapter(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String getParameter(String name) {
        return this.request.getParameter(name);
    }
    // ...
}
```
4. 在需要适用 `HttpServletRequest` 的地方，替换为

```java
new JakartaRequestAdapter(HttpServletRequest);
```

## 支持的所有适配接口/类

```java
// 适配 Request
JapHttpRequest
// 适配 Cookie
JapHttpCookie
// 适配 Session
JapHttpSession
// 适配 Response
JapHttpResponse
```

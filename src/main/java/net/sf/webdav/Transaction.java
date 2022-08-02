package net.sf.webdav;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;

public class Transaction implements ITransaction {
    private final Principal principal;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public Transaction(Principal principal, HttpServletRequest request, HttpServletResponse response) {
        this.principal = principal;
        this.request = request;
        this.response = response;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public HttpServletResponse getResponse() {
        return response;
    }
}

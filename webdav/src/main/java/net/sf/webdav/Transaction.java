package net.sf.webdav;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;

import java.security.Principal;

public class Transaction implements ITransaction {
    private final Principal principal;
    private final JapHttpRequest request;
    private final JapHttpResponse response;

    public Transaction(Principal principal, JapHttpRequest request, JapHttpResponse response) {
        this.principal = principal;
        this.request = request;
        this.response = response;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public JapHttpRequest getRequest() {
        return request;
    }

    @Override
    public JapHttpResponse getResponse() {
        return response;
    }
}

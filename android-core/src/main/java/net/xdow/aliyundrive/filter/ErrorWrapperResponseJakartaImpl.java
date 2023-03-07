package net.xdow.aliyundrive.filter;

import com.github.zxbu.webdavteambition.filter.IErrorWrapperResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;

public class ErrorWrapperResponseJakartaImpl extends HttpServletResponseWrapper implements IErrorWrapperResponse {
    private int status;
    private String message;
    private boolean hasErrorToSend = false;

    public ErrorWrapperResponseJakartaImpl(HttpServletResponse response) {
        super(response);
    }

    public void sendError(int status) throws IOException {
        this.sendError(status, (String) null);
    }

    public void sendError(int status, String message) throws IOException {
        this.status = status;
        this.message = message;
        this.hasErrorToSend = true;
    }

    public int getStatus() {
        return this.hasErrorToSend ? this.status : super.getStatus();
    }

    public void flushBuffer() throws IOException {
        super.flushBuffer();
    }


    public String getMessage() {
        return this.message;
    }

    public boolean hasErrorToSend() {
        return this.hasErrorToSend;
    }

}
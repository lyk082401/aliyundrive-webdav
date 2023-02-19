package com.github.zxbu.webdavteambition.filter;

import jakarta.servlet.annotation.WebFilter;
import net.sf.webdav.WebdavStatus;

import org.apache.commons.io.IOUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@WebFilter(urlPatterns = "/s/*")
public class ErrorFilter implements Filter {
    private static final String errorPage = readErrorPage();

    private static String readErrorPage() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<D:multistatus xmlns:D='DAV:'>\n" +
                "    <D:response>\n" +
                "        <D:href></D:href>\n" +
                "        <D:propstat>\n" +
                "            <D:status>HTTP/1.1 {{code}} {{message}}</D:status>\n" +
                "        </D:propstat>\n" +
                "    </D:response>\n" +
                "</D:multistatus>";
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse && request instanceof HttpServletRequest) {
        } else {
            return;
        }
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        ErrorWrapperResponse wrapperResponse = new ErrorWrapperResponse(httpServletResponse);
        try {
            filterChain.doFilter(httpServletRequest, wrapperResponse);
            if (wrapperResponse.hasErrorToSend()) {
                int status = wrapperResponse.getStatus();
                if (status == 401) {
//                    httpServletResponse.addHeader("WWW-Authenticate", "Digest realm=\"iptel.org\", qop=\"auth,auth-int\",\n" +
//                            "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", opaque=\"\", algorithm=MD5");
//
                }
                httpServletResponse.setStatus(status);
                String message = wrapperResponse.getMessage();
                if (message == null) {
                    message = WebdavStatus.getStatusText(status);
                }
                String errorXml = errorPage.replace("{{code}}", status + "").replace("{{message}}", message);
                httpServletResponse.getOutputStream().write(errorXml.getBytes(StandardCharsets.UTF_8));
            }
            httpServletResponse.flushBuffer();
        } catch (Throwable t) {
            httpServletResponse.setStatus(500);
            try {
                httpServletResponse.setStatus(500);
                httpServletResponse.getOutputStream().write(t.getMessage().getBytes(StandardCharsets.UTF_8));
                httpServletResponse.flushBuffer();
            } catch (IOException e) {
            }
        }
    }

    private static class ErrorWrapperResponse extends HttpServletResponseWrapper {
        private int status;
        private String message;
        private boolean hasErrorToSend = false;

        ErrorWrapperResponse(HttpServletResponse response) {
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


        String getMessage() {
            return this.message;
        }

        boolean hasErrorToSend() {
            return this.hasErrorToSend;
        }

        public PrintWriter getWriter() throws IOException {
            return super.getWriter();
        }

        public ServletOutputStream getOutputStream() throws IOException {
            return super.getOutputStream();
        }
    }
}

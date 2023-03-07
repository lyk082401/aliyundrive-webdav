package com.github.zxbu.webdavteambition.filter;

import com.fujieid.jap.http.adapter.jakarta.JakartaRequestAdapter;
import com.fujieid.jap.http.adapter.jakarta.JakartaResponseAdapter;
import com.github.zxbu.webdavteambition.filter.impl.ErrorFilterImpl;
import com.github.zxbu.webdavteambition.filter.impl.ErrorWrapperResponseJakartaImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ErrorFilter extends OncePerRequestFilter implements IErrorFilterCall {
    private final IErrorFilter mErrorFilter = new ErrorFilterImpl(this);

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain chain) throws ServletException, IOException {
        JakartaRequestAdapter req = new JakartaRequestAdapter(httpServletRequest);
        JakartaResponseAdapter res = new JakartaResponseAdapter(httpServletResponse);
        ErrorWrapperResponseJakartaImpl wrapperResponse = new ErrorWrapperResponseJakartaImpl(httpServletResponse);
        chain.doFilter(httpServletRequest, wrapperResponse);
        this.mErrorFilter.doFilterCall(req, res, wrapperResponse);
    }

    public String readErrorPage() {
        try {
            ClassPathResource classPathResource = new ClassPathResource("error.xml");
            InputStream inputStream = classPathResource.getInputStream();
            byte[] buffer = new byte[(int) classPathResource.contentLength()];
            IOUtils.readFully(inputStream, buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}

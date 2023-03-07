package com.github.zxbu.webdavteambition.filter;

import com.github.zxbu.webdavteambition.config.AliyunDriveProperties;
import com.github.zxbu.webdavteambition.config.AliyunDrivePropertiesSpring;
import com.github.zxbu.webdavteambition.store.AliyunDriveClientService;
import com.github.zxbu.webdavteambition.util.AliyunDriveClientServiceHolder;
import com.github.zxbu.webdavteambition.util.SpringBeanFactory;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sf.webdav.WebdavStatus;
import net.xdow.aliyundrive.IAliyunDrive;
import net.xdow.aliyundrive.bean.AliyunDriveEnum;
import net.xdow.aliyundrive.bean.AliyunDriveRequest;
import net.xdow.aliyundrive.bean.AliyunDriveResponse;
import net.xdow.aliyundrive.exception.NotAuthorizeException;
import net.xdow.aliyundrive.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.Locale;

public class AliyunDriveLoginFilter implements Filter {


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            if ("/".equals(httpRequest.getRequestURI())) {
                String accessToken = httpRequest.getParameter("access_token");
                String refreshToken = httpRequest.getParameter("refresh_token");
                String tokenType = httpRequest.getParameter("token_type");
                String expiresIn = httpRequest.getParameter("expires_in");
                if (!StringUtils.isEmpty(accessToken) && !StringUtils.isEmpty(refreshToken)
                        && !StringUtils.isEmpty(tokenType) && !StringUtils.isEmpty(expiresIn)) {
                    AliyunDriveResponse.AccessTokenInfo accessTokenInfo = new AliyunDriveResponse.AccessTokenInfo();
                    accessTokenInfo.setAccessToken(accessToken);
                    accessTokenInfo.setRefreshToken(refreshToken);
                    accessTokenInfo.setTokenType(tokenType);
                    accessTokenInfo.setExpiresIn(expiresIn);
                    AliyunDriveClientServiceHolder serviceHolder = SpringBeanFactory.getBean(AliyunDriveClientServiceHolder.class);
                    AliyunDriveClientService service = serviceHolder.getAliyunDriveClientService();
                    service.getAliyunDrive().setAccessTokenInfo(accessTokenInfo);
                    service.getProperties().save(accessTokenInfo);
                    service.onAccountChanged();
                    if (response instanceof HttpServletResponse) {
                        ((HttpServletResponse) response).sendRedirect("/");
                        return;
                    }
                } else if (!StringUtils.isEmpty(refreshToken)) {
                    AliyunDriveClientServiceHolder serviceHolder = SpringBeanFactory.getBean(AliyunDriveClientServiceHolder.class);
                    AliyunDriveClientService service = serviceHolder.getAliyunDriveClientService();
                    IAliyunDrive aliyunDrive = service.getAliyunDrive();
                    AliyunDriveRequest.AccessTokenInfo query = new AliyunDriveRequest.AccessTokenInfo();
                    query.setGrantType(AliyunDriveEnum.GrantType.RefreshToken);
                    query.setRefreshToken(refreshToken);
                    AliyunDriveResponse.AccessTokenInfo accessTokenInfo = aliyunDrive.getAccessToken(query)
                            .disableAuthorizeCheck().execute();
                    if (accessTokenInfo.isError()) {
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                accessTokenInfo.getMessage() + "(" + accessTokenInfo.getCode() + ")");
                        return;
                    }
                    aliyunDrive.setAccessTokenInfo(accessTokenInfo);
                    service.getProperties().save(accessTokenInfo);
                    service.onAccountChanged();
                    if (response instanceof HttpServletResponse) {
                        ((HttpServletResponse) response).sendRedirect("/");
                        return;
                    }
                }
            }
        }

        chain.doFilter(request, response);
        if (response instanceof IErrorWrapperResponse) {
            IErrorWrapperResponse resp = (IErrorWrapperResponse) response;
            if (resp.getStatus() == WebdavStatus.SC_INTERNAL_SERVER_ERROR) {
                if (String.valueOf(resp.getMessage()).contains(NotAuthorizeException.class.getName())) {
                    //开始登录
                    String rootUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                    AliyunDrivePropertiesSpring properties = SpringBeanFactory.getBean(AliyunDrivePropertiesSpring.class);
                    if (properties.getDriver() == AliyunDriveProperties.Driver.OpenApi) {
                        String loginUrl = String.format(Locale.getDefault(), properties.getAliyunAuthorizeUrl(), rootUrl);
                        resp.sendRedirect(loginUrl);
//                    resp.sendError(WebdavStatus.SC_UNAUTHORIZED, "AliyunDriveAccessTokenInvalid");
                        resp.sendError(WebdavStatus.SC_UNAUTHORIZED,
                                "授权已失效, 请<a href=\"" + loginUrl + "\">点击链接</a>前往获取授权. ");
                    }
                }
            }
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}

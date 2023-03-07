package net.sf.webdav.servlet;

public interface IWebdavServlet {
    void init(Object[] args, IWebdavServletCall servletCall, IWebdavServletBean iWebdavServletBean);
    public interface IWebdavServletCall {
        String getInitParameter(String path);
        String getInitParameterFromServletConfig(String path);
    }
}

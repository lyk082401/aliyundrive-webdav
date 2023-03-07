package net.sf.webdav.servlet;

import com.fujieid.jap.http.JapHttpRequest;
import com.fujieid.jap.http.JapHttpResponse;
import net.sf.webdav.IWebdavStore;

public interface IWebdavServletBean {

    void init(IWebdavServletBeanServletCall iWebdavServletBeanServletCall);
    void init(IWebdavStore store, String dftIndexFile,
              String insteadOf404, int nocontentLenghHeaders,
              boolean lazyFolderCreationOnPut);
    void destroy();
    void service(JapHttpRequest req, JapHttpResponse resp) throws Exception;

    public interface IWebdavServletBeanServletCall {
        String getMimeType(String path);
    }
}

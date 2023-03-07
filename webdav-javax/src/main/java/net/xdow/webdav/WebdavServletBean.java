package net.xdow.webdav;

import com.fujieid.jap.http.JapException;
import com.fujieid.jap.http.adapter.javax.JavaxRequestAdapter;
import com.fujieid.jap.http.adapter.javax.JavaxResponseAdapter;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.servlet.IWebdavServletBean;
import net.sf.webdav.servlet.impl.WebdavServletBeanImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
public class WebdavServletBean extends HttpServlet implements IWebdavServletBean.IWebdavServletBeanServletCall {

    public IWebdavServletBean mWebdavServletBean = new WebdavServletBeanImpl();

    public WebdavServletBean() {
        this.mWebdavServletBean.init(this);
    }

    public void init(IWebdavStore store, String dftIndexFile,
                     String insteadOf404, int nocontentLenghHeaders,
                     boolean lazyFolderCreationOnPut) throws JapException {
        this.mWebdavServletBean.init(store, dftIndexFile, insteadOf404,
                nocontentLenghHeaders, lazyFolderCreationOnPut);
    }
    @Override
    public void destroy() {
        this.mWebdavServletBean.destroy();
        super.destroy();
    }


    /**
     * Handles the special WebDAV methods.
     */
    @Override
    protected void service(HttpServletRequest _req, HttpServletResponse _resp) throws ServletException, IOException {
        JavaxRequestAdapter req = new JavaxRequestAdapter(_req);
        JavaxResponseAdapter resp = new JavaxResponseAdapter(_resp);
        try {
            this.mWebdavServletBean.service(req, resp);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public String getMimeType(String path) {
        return getServletContext().getMimeType(path);
    }
}

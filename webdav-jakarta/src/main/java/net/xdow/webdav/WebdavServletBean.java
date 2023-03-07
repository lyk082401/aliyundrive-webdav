package net.xdow.webdav;

import com.fujieid.jap.http.JapException;
import com.fujieid.jap.http.adapter.jakarta.JakartaRequestAdapter;
import com.fujieid.jap.http.adapter.jakarta.JakartaResponseAdapter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.servlet.IWebdavServletBean;
import net.sf.webdav.servlet.impl.WebdavServletBeanImpl;

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
        JakartaRequestAdapter req = new JakartaRequestAdapter(_req);
        JakartaResponseAdapter resp = new JakartaResponseAdapter(_resp);
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

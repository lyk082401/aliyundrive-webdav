package net.sf.webdav.servlet.impl;

import net.sf.webdav.IWebdavStore;
import net.sf.webdav.LocalFileSystemStore;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.servlet.IWebdavServlet;
import net.sf.webdav.servlet.IWebdavServletBean;

import java.io.File;
import java.lang.reflect.Constructor;

public class WebdavServletImpl implements IWebdavServlet {
    private static final String ROOTPATH_PARAMETER = "rootpath";

    private IWebdavServletCall mWebdavServletCall;

    private Object[] mArgs;

    @Override
    public void init(Object[] args, IWebdavServletCall iWebdavServletCall, IWebdavServletBean iWebdavServletBean) {
        this.mArgs = args;
        this.mWebdavServletCall = iWebdavServletCall;
        // Parameters from web.xml
        String clazzName = iWebdavServletCall.getInitParameterFromServletConfig(
                "ResourceHandlerImplementation");
        if (clazzName == null || clazzName.equals("")) {
            clazzName = LocalFileSystemStore.class.getName();
        }

        File root = getFileRoot();

        IWebdavStore webdavStore = constructStore(clazzName, mArgs, root);

        boolean lazyFolderCreationOnPut = iWebdavServletCall.getInitParameter("lazyFolderCreationOnPut") != null
                && iWebdavServletCall.getInitParameter("lazyFolderCreationOnPut").equals("1");

        String dftIndexFile = iWebdavServletCall.getInitParameter("default-index-file");
        String insteadOf404 = iWebdavServletCall.getInitParameter("instead-of-404");

        int noContentLengthHeader = getIntInitParameter("no-content-length-headers");
        iWebdavServletBean.init(webdavStore, dftIndexFile, insteadOf404,
                noContentLengthHeader, lazyFolderCreationOnPut);
    }

    private int getIntInitParameter(String key) {
        return this.mWebdavServletCall.getInitParameter(key) == null ? -1 : Integer
                .parseInt(this.mWebdavServletCall.getInitParameter(key));
    }

    protected IWebdavStore constructStore(String clazzName, Object[] args, File root) {
        IWebdavStore webdavStore;
        try {
            Class<?> clazz = WebdavServletImpl.class.getClassLoader().loadClass(
                    clazzName);

            Constructor<?> ctor = clazz
                    .getConstructor(new Class[]{Object[].class, File.class});

            webdavStore = (IWebdavStore) ctor
                    .newInstance(new Object[]{args, root});
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("some problem making store component", e);
        }
        return webdavStore;
    }

    private File getFileRoot() {
        String rootPath = this.mWebdavServletCall.getInitParameter(ROOTPATH_PARAMETER);
        if (rootPath == null) {
            throw new WebdavException("missing parameter: "
                    + ROOTPATH_PARAMETER);
        }
        if (rootPath.equals("*WAR-FILE-ROOT*")) {
            String file = LocalFileSystemStore.class.getProtectionDomain()
                    .getCodeSource().getLocation().getFile().replace('\\', '/');
            if (file.charAt(0) == '/'
                    && System.getProperty("os.name").indexOf("Windows") != -1) {
                file = file.substring(1, file.length());
            }

            int ix = file.indexOf("/WEB-INF/");
            if (ix != -1) {
                rootPath = file.substring(0, ix).replace('/',
                        File.separatorChar);
            } else {
                throw new WebdavException(
                        "Could not determine root of war file. Can't extract from path '"
                                + file + "' for this web container");
            }
        }
        return new File(rootPath);
    }
}

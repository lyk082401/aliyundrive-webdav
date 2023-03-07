/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.xdow.webdav;

import net.sf.webdav.servlet.IWebdavServlet;
import net.sf.webdav.servlet.impl.WebdavServletImpl;

/**
 * Servlet which provides support for WebDAV level 2.
 * <p>
 * the original class is org.apache.catalina.servlets.WebdavServlet by Remy
 * Maucherat, which was heavily changed
 *
 * @author Remy Maucherat
 */

public class WebdavServlet extends WebdavServletBean implements IWebdavServlet.IWebdavServletCall {

    private IWebdavServlet mWebdavServlet = new WebdavServletImpl();
    private final Object[] mArgs;
    public WebdavServlet(Object... args) {
        mArgs = args;
    }

    public void init() {
        mWebdavServlet.init(this.mArgs, this, mWebdavServletBean);
    }

    @Override
    public String getInitParameterFromServletConfig(String path) {
        return getServletConfig().getInitParameter(path);
    }
}

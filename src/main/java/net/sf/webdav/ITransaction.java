package net.sf.webdav;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;

public interface ITransaction {

    Principal getPrincipal();

    HttpServletRequest getRequest();

    HttpServletResponse getResponse();
}

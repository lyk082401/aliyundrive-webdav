package com.github.zxbu.webdavteambition.filter;

import java.io.IOException;
import java.util.Collection;

public interface IErrorWrapperResponse  {
    void flushBuffer() throws IOException;
    String getMessage();
    boolean hasErrorToSend();

    /**
     * Returns a boolean indicating whether the named response header has
     * already been set.
     *
     * @param name
     *            the header name
     * @return <code>true</code> if the named response header has already been
     *         set; <code>false</code> otherwise
     */
    public boolean containsHeader(String name);

    /**
     * Encodes the specified URL by including the session ID in it, or, if
     * encoding is not needed, returns the URL unchanged. The implementation of
     * this method includes the logic to determine whether the session ID needs
     * to be encoded in the URL. For example, if the browser supports cookies,
     * or session tracking is turned off, URL encoding is unnecessary.
     * <p>
     * For robust session tracking, all URLs emitted by a servlet should be run
     * through this method. Otherwise, URL rewriting cannot be used with
     * browsers which do not support cookies.
     *
     * @param url
     *            the url to be encoded.
     * @return the encoded URL if encoding is needed; the unchanged URL
     *         otherwise.
     */
    public String encodeURL(String url);

    /**
     * Encodes the specified URL for use in the <code>sendRedirect</code> method
     * or, if encoding is not needed, returns the URL unchanged. The
     * implementation of this method includes the logic to determine whether the
     * session ID needs to be encoded in the URL. Because the rules for making
     * this determination can differ from those used to decide whether to encode
     * a normal link, this method is separated from the <code>encodeURL</code>
     * method.
     * <p>
     * All URLs sent to the <code>HttpServletResponse.sendRedirect</code> method
     * should be run through this method. Otherwise, URL rewriting cannot be
     * used with browsers which do not support cookies.
     *
     * @param url
     *            the url to be encoded.
     * @return the encoded URL if encoding is needed; the unchanged URL
     *         otherwise.
     * @see #sendRedirect
     */
    public String encodeRedirectURL(String url);

    /**
     * Sends an error response to the client using the specified status code and
     * clears the output buffer. The server defaults to creating the response to
     * look like an HTML-formatted server error page containing the specified
     * message, setting the content type to "text/html", leaving cookies and
     * other headers unmodified. If an error-page declaration has been made for
     * the web application corresponding to the status code passed in, it will
     * be served back in preference to the suggested msg parameter.
     * <p>
     * If the response has already been committed, this method throws an
     * IllegalStateException. After using this method, the response should be
     * considered to be committed and should not be written to.
     *
     * @param sc
     *            the error status code
     * @param msg
     *            the descriptive message
     * @exception IOException
     *                If an input or output exception occurs
     * @exception IllegalStateException
     *                If the response was committed
     */
    public void sendError(int sc, String msg) throws IOException;

    /**
     * Sends an error response to the client using the specified status code and
     * clears the buffer. This is equivalent to calling {@link #sendError(int,
     * String)} with the same status code and <code>null</code> for the message.
     *
     * @param sc
     *            the error status code
     * @exception IOException
     *                If an input or output exception occurs
     * @exception IllegalStateException
     *                If the response was committed before this method call
     */
    public void sendError(int sc) throws IOException;

    /**
     * Sends a temporary redirect response to the client using the specified
     * redirect location URL. This method can accept relative URLs; the servlet
     * container must convert the relative URL to an absolute URL before sending
     * the response to the client. If the location is relative without a leading
     * '/' the container interprets it as relative to the current request URI.
     * If the location is relative with a leading '/' the container interprets
     * it as relative to the servlet container root.
     * <p>
     * If the response has already been committed, this method throws an
     * IllegalStateException. After using this method, the response should be
     * considered to be committed and should not be written to.
     *
     * @param location
     *            the redirect location URL
     * @exception IOException
     *                If an input or output exception occurs
     * @exception IllegalStateException
     *                If the response was committed or if a partial URL is given
     *                and cannot be converted into a valid URL
     */
    public void sendRedirect(String location) throws IOException;

    /**
     * Sets a response header with the given name and date-value. The date is
     * specified in terms of milliseconds since the epoch. If the header had
     * already been set, the new value overwrites the previous one. The
     * <code>containsHeader</code> method can be used to test for the presence
     * of a header before setting its value.
     *
     * @param name
     *            the name of the header to set
     * @param date
     *            the assigned date value
     * @see #containsHeader
     * @see #addDateHeader
     */
    public void setDateHeader(String name, long date);

    /**
     * Adds a response header with the given name and date-value. The date is
     * specified in terms of milliseconds since the epoch. This method allows
     * response headers to have multiple values.
     *
     * @param name
     *            the name of the header to set
     * @param date
     *            the additional date value
     * @see #setDateHeader
     */
    public void addDateHeader(String name, long date);

    /**
     * Sets a response header with the given name and value. If the header had
     * already been set, the new value overwrites the previous one. The
     * <code>containsHeader</code> method can be used to test for the presence
     * of a header before setting its value.
     *
     * @param name
     *            the name of the header
     * @param value
     *            the header value If it contains octet string, it should be
     *            encoded according to RFC 2047
     *            (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #containsHeader
     * @see #addHeader
     */
    public void setHeader(String name, String value);

    /**
     * Adds a response header with the given name and value. This method allows
     * response headers to have multiple values.
     *
     * @param name
     *            the name of the header
     * @param value
     *            the additional header value If it contains octet string, it
     *            should be encoded according to RFC 2047
     *            (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #setHeader
     */
    public void addHeader(String name, String value);

    /**
     * Sets a response header with the given name and integer value. If the
     * header had already been set, the new value overwrites the previous one.
     * The <code>containsHeader</code> method can be used to test for the
     * presence of a header before setting its value.
     *
     * @param name
     *            the name of the header
     * @param value
     *            the assigned integer value
     * @see #containsHeader
     * @see #addIntHeader
     */
    public void setIntHeader(String name, int value);

    /**
     * Adds a response header with the given name and integer value. This method
     * allows response headers to have multiple values.
     *
     * @param name
     *            the name of the header
     * @param value
     *            the assigned integer value
     * @see #setIntHeader
     */
    public void addIntHeader(String name, int value);

    /**
     * Sets the status code for this response. This method is used to set the
     * return status code when there is no error (for example, for the status
     * codes SC_OK or SC_MOVED_TEMPORARILY). If there is an error, and the
     * caller wishes to invoke an error-page defined in the web application, the
     * <code>sendError</code> method should be used instead.
     * <p>
     * The container clears the buffer and sets the Location header, preserving
     * cookies and other headers.
     *
     * @param sc
     *            the status code
     * @see #sendError
     */
    public void setStatus(int sc);

    /**
     * Get the HTTP status code for this Response.
     *
     * @return The HTTP status code for this Response
     *
     * @since Servlet 3.0
     */
    public int getStatus();

    /**
     * Return the value for the specified header, or <code>null</code> if this
     * header has not been set.  If more than one value was added for this
     * name, only the first is returned; use {@link #getHeaders(String)} to
     * retrieve all of them.
     *
     * @param name Header name to look up
     *
     * @return The first value for the specified header. This is the raw value
     *         so if multiple values are specified in the first header then they
     *         will be returned as a single header value .
     *
     * @since Servlet 3.0
     */
    String getHeader(String name);

    /**
     * Return a Collection of all the header values associated with the
     * specified header name.
     *
     * @param name Header name to look up
     *
     * @return The values for the specified header. These are the raw values so
     *         if multiple values are specified in a single header that will be
     *         returned as a single header value.
     *
     * @since Servlet 3.0
     */
    Collection<String> getHeaders(String name);

    /**
     * Get the header names set for this HTTP response.
     *
     * @return The header names set for this HTTP response.
     *
     * @since Servlet 3.0
     */
    Collection<String> getHeaderNames();

}

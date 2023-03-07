package net.sf.webdav;

public interface IMimeTyper {

    /**
     * Detect the mime type of this object
     *
     */
    String getMimeType(ITransaction transaction, String path);
}

package com.fujieid.jap.http;

public class JapException extends Exception {

    public JapException() {
    }

    public JapException(String message) {
        super(message);
    }

    public JapException(String message, Throwable cause) {
        super(message, cause);
    }

    public JapException(Throwable cause) {
        super(cause);
    }

    public JapException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

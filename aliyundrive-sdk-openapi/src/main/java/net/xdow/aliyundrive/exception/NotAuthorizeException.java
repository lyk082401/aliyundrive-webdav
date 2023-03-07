package net.xdow.aliyundrive.exception;

public class NotAuthorizeException extends IllegalStateException {
    public NotAuthorizeException() {
    }

    public NotAuthorizeException(String s) {
        super(s);
    }

    public NotAuthorizeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotAuthorizeException(Throwable cause) {
        super(cause);
    }
}

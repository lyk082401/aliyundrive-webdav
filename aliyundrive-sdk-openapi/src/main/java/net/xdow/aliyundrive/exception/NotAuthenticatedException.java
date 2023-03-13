package net.xdow.aliyundrive.exception;

public class NotAuthenticatedException extends IllegalStateException {
    public NotAuthenticatedException() {
    }

    public NotAuthenticatedException(String s) {
        super(s);
    }

    public NotAuthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotAuthenticatedException(Throwable cause) {
        super(cause);
    }
}

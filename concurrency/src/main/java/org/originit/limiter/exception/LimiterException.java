package org.originit.limiter.exception;

public class LimiterException extends RuntimeException{

    public LimiterException() {
    }

    public LimiterException(String message) {
        super(message);
    }

    public LimiterException(String message, Throwable cause) {
        super(message, cause);
    }

    public LimiterException(Throwable cause) {
        super(cause);
    }

    public LimiterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

package org.originit.limiter.exception;

public class LeakyBucketFullException extends LimiterException{

    public LeakyBucketFullException() {
    }

    public LeakyBucketFullException(String message) {
        super(message);
    }

    public LeakyBucketFullException(String message, Throwable cause) {
        super(message, cause);
    }

    public LeakyBucketFullException(Throwable cause) {
        super(cause);
    }

    public LeakyBucketFullException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

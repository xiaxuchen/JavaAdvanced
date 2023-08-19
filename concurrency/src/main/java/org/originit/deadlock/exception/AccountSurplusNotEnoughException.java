package org.originit.deadlock.exception;

public class AccountSurplusNotEnoughException extends RuntimeException {

    public AccountSurplusNotEnoughException() {
    }

    public AccountSurplusNotEnoughException(String message) {
        super(message);
    }

    public AccountSurplusNotEnoughException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountSurplusNotEnoughException(Throwable cause) {
        super(cause);
    }

    public AccountSurplusNotEnoughException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

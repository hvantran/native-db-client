package com.hvantran.sqlnative.exceptions;

/**
 * Generic application exception, all the exception must be inherited from {@link AppException}
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(Throwable throwable) {
        super(throwable);
    }

    public AppException(String message, Throwable throwable) {
        super(message, throwable);
    }
}

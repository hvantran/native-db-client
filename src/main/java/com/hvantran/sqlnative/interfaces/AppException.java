package com.hvantran.sqlnative.interfaces;

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

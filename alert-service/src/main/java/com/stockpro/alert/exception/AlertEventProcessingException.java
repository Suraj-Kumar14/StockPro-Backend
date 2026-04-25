package com.stockpro.alert.exception;

public class AlertEventProcessingException extends RuntimeException {

    public AlertEventProcessingException(String message) {
        super(message);
    }

    public AlertEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

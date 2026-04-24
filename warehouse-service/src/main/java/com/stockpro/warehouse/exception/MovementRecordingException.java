package com.stockpro.warehouse.exception;

public class MovementRecordingException extends RuntimeException {

    public MovementRecordingException(String message) {
        super(message);
    }

    public MovementRecordingException(String message, Throwable cause) {
        super(message, cause);
    }
}

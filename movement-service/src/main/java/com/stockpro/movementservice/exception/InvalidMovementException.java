package com.stockpro.movementservice.exception;

public class InvalidMovementException extends RuntimeException {

    public InvalidMovementException(String message) {
        super(message);
    }
}

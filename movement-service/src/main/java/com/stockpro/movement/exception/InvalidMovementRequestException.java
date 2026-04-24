package com.stockpro.movement.exception;

public class InvalidMovementRequestException extends RuntimeException {

    public InvalidMovementRequestException(String message) {
        super(message);
    }
}

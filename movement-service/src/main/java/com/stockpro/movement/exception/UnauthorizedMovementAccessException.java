package com.stockpro.movement.exception;

public class UnauthorizedMovementAccessException extends RuntimeException {

    public UnauthorizedMovementAccessException(String message) {
        super(message);
    }
}

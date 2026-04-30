package com.stockpro.purchaseservice.exception;

public class InvalidPOStateException extends RuntimeException {

    public InvalidPOStateException(String message) {
        super(message);
    }
}

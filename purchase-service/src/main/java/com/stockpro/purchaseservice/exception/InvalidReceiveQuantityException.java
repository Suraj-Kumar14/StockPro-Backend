package com.stockpro.purchaseservice.exception;

public class InvalidReceiveQuantityException extends RuntimeException {
    public InvalidReceiveQuantityException(String message) {
        super(message);
    }
}

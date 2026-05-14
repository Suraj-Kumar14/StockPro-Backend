package com.stockpro.purchaseservice.exception;

public class InvalidLineItemException extends RuntimeException {

    public InvalidLineItemException(String message) {
        super(message);
    }
}

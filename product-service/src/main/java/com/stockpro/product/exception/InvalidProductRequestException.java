package com.stockpro.product.exception;

public class InvalidProductRequestException extends RuntimeException {

    public InvalidProductRequestException(String message) {
        super(message);
    }
}

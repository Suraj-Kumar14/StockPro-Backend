package com.stockpro.product_service.exception;

public class InvalidProductDataException extends RuntimeException {

    public InvalidProductDataException(String message) {
        super(message);
    }
}

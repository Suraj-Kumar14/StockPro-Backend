package com.stockpro.product_service.exception;

public class DuplicateBarcodeException extends RuntimeException {

    public DuplicateBarcodeException(String message) {
        super(message);
    }
}

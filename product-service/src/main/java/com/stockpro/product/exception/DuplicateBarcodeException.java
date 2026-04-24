package com.stockpro.product.exception;

public class DuplicateBarcodeException extends RuntimeException {

    public DuplicateBarcodeException(String message) {
        super(message);
    }
}

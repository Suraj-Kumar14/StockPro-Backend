package com.stockpro.purchaseservice.exception;

public class OverReceiptException extends RuntimeException {

    public OverReceiptException(String message) {
        super(message);
    }
}

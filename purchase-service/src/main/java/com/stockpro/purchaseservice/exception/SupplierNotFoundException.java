package com.stockpro.purchaseservice.exception;

public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(String message) {
        super(message);
    }
}

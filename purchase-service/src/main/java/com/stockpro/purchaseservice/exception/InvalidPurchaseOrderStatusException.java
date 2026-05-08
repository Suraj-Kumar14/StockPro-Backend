package com.stockpro.purchaseservice.exception;

public class InvalidPurchaseOrderStatusException extends RuntimeException {
    public InvalidPurchaseOrderStatusException(String message) {
        super(message);
    }
}

package com.stockpro.supplierservice.exception;

public class DuplicateSupplierException extends RuntimeException {

    public DuplicateSupplierException(String message) {
        super(message);
    }
}

package com.stockpro.supplierservice.exception;

@Deprecated
public class DuplicateTaxIdException extends DuplicateSupplierException {
    public DuplicateTaxIdException(String message) {
        super(message);
    }
}

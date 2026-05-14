package com.stockpro.warehouseservice.exception;

public class CapacityExceededException extends RuntimeException {

    public CapacityExceededException(String message) {
        super(message);
    }
}

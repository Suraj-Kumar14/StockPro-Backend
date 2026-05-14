package com.stockpro.warehouseservice.exception;

public class StockNotAvailableException extends RuntimeException {

    public StockNotAvailableException(String message) {
        super(message);
    }
}

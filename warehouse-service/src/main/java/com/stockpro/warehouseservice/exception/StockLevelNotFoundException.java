package com.stockpro.warehouseservice.exception;

public class StockLevelNotFoundException extends RuntimeException {
    public StockLevelNotFoundException(String message) {
        super(message);
    }
}

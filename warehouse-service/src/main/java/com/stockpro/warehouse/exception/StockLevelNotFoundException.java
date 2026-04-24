package com.stockpro.warehouse.exception;

public class StockLevelNotFoundException extends RuntimeException {

    public StockLevelNotFoundException(String message) {
        super(message);
    }
}

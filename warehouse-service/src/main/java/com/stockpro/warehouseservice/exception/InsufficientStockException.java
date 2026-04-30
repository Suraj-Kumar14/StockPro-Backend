package com.stockpro.warehouseservice.exception;

public class InsufficientStockException extends StockNotAvailableException {
    public InsufficientStockException(String message) {
        super(message);
    }
}

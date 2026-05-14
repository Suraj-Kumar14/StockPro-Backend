package com.stockpro.purchaseservice.exception;

import org.springframework.http.HttpStatus;

public class WarehouseStockUpdateException extends RuntimeException {
    private final HttpStatus status;

    public WarehouseStockUpdateException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public WarehouseStockUpdateException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

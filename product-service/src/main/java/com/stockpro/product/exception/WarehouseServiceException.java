package com.stockpro.product.exception;

/**
 * Thrown when product-service cannot get live stock data from warehouse-service.
 */
public class WarehouseServiceException extends RuntimeException {

    public WarehouseServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

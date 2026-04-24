package com.stockpro.product.exception;

/**
 * Thrown when a product record is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

package com.stockpro.product.exception;

/**
 * Thrown when a unique product field like SKU or barcode already exists.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

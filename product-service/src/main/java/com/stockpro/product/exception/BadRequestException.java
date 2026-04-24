package com.stockpro.product.exception;

/**
 * Thrown when request data breaks a business rule.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}

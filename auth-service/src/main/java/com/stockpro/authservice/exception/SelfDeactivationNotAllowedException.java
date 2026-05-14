package com.stockpro.authservice.exception;

public class SelfDeactivationNotAllowedException extends RuntimeException {
    public SelfDeactivationNotAllowedException(String message) {
        super(message);
    }
}

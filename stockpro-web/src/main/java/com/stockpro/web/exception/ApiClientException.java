package com.stockpro.web.exception;

public class ApiClientException extends RuntimeException {

    private final int statusCode;
    private final String operation;

    public ApiClientException(int statusCode, String operation, String message) {
        super(message);
        this.statusCode = statusCode;
        this.operation = operation;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getOperation() {
        return operation;
    }
}

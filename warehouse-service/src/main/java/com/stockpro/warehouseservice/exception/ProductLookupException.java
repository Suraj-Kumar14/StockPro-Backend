package com.stockpro.warehouseservice.exception;

public class ProductLookupException extends RuntimeException {

    private final boolean upstreamFailure;

    public ProductLookupException(String message) {
        this(message, false);
    }

    public ProductLookupException(String message, boolean upstreamFailure) {
        super(message);
        this.upstreamFailure = upstreamFailure;
    }

    public boolean isUpstreamFailure() {
        return upstreamFailure;
    }
}

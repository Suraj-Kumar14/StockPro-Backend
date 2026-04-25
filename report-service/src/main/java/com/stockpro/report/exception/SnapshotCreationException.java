package com.stockpro.report.exception;

public class SnapshotCreationException extends RuntimeException {

    public SnapshotCreationException(String message) {
        super(message);
    }

    public SnapshotCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.stockpro.warehouseservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WarehouseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            WarehouseNotFoundException ex, HttpServletRequest request) {
        log.error("WarehouseNotFoundException: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "WAREHOUSE_NOT_FOUND",
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(ProductLookupException.class)
    public ResponseEntity<ErrorResponse> handleProductLookup(
            ProductLookupException ex, HttpServletRequest request) {
        log.error("ProductLookupException: {}", ex.getMessage());
        HttpStatus status = ex.isUpstreamFailure()
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.NOT_FOUND;
        String errorCode = ex.isUpstreamFailure() ? "PRODUCT_SERVICE_UNAVAILABLE" : "PRODUCT_NOT_FOUND";
        return buildError(status, errorCode, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(StockLevelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStockLevelNotFound(
            StockLevelNotFoundException ex, HttpServletRequest request) {
        log.error("StockLevelNotFoundException: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "STOCK_LEVEL_NOT_FOUND",
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler({InsufficientStockException.class, StockNotAvailableException.class})
    public ResponseEntity<ErrorResponse> handleStockNotAvailable(
            RuntimeException ex, HttpServletRequest request) {
        log.error("StockNotAvailableException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "INSUFFICIENT_STOCK",
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(CapacityExceededException.class)
    public ResponseEntity<ErrorResponse> handleCapacityExceeded(
            CapacityExceededException ex, HttpServletRequest request) {
        log.error("CapacityExceededException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "WAREHOUSE_CAPACITY_EXCEEDED",
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(
            InvalidOperationException ex, HttpServletRequest request) {
        log.error("InvalidOperationException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_STOCK_OPERATION",
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", request.getRequestURI(), errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.error("ConstraintViolationException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error("IllegalArgumentException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator/")) {
            log.warn("Actuator endpoint not available: {}", path);
        } else {
            log.warn("Resource not found: {}", path);
        }
        return buildError(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "Resource not found", path, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("DataIntegrityViolationException: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "Duplicate resource or conflicting data", request.getRequestURI(), null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockConflict(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.error("Optimistic locking conflict: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
                "Stock was updated by another transaction. Please retry.",
                request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred", request.getRequestURI(), null);
    }

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String errorCode,
            String message,
            String path,
            Map<String, String> validationErrors) {
        return new ResponseEntity<>(
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .errorCode(errorCode)
                        .message(message)
                        .path(path)
                        .validationErrors(validationErrors == null || validationErrors.isEmpty() ? null : validationErrors)
                        .build(),
                status);
    }

}

package com.stockpro.product_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFoundException(
            ProductNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Product not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler({DuplicateSkuException.class, DuplicateBarcodeException.class})
    public ResponseEntity<ErrorResponse> handleConflictExceptions(
            RuntimeException ex,
            HttpServletRequest request) {
        log.warn("Conflict detected: {}", ex.getMessage());
        String errorCode = ex instanceof DuplicateSkuException ? "DUPLICATE_SKU" : "DUPLICATE_BARCODE";
        return buildErrorResponse(HttpStatus.CONFLICT, errorCode, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidProductDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProductDataException(
            InvalidProductDataException ex,
            HttpServletRequest request) {
        log.warn("Invalid product data: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });

        return buildErrorResponse(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Validation failed", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (message != null) {
            String normalized = message.toLowerCase();
            if (normalized.contains("sku")) {
                return buildErrorResponse(HttpStatus.CONFLICT, "DUPLICATE_SKU", "Duplicate SKU number", request, Map.of("sku", "Duplicate SKU number"));
            }
            if (normalized.contains("barcode")) {
                return buildErrorResponse(HttpStatus.CONFLICT, "DUPLICATE_BARCODE", "Duplicate barcode", request, Map.of("barcode", "Duplicate barcode"));
            }
        }
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "DATA_INTEGRITY_VIOLATION",
                "Product data conflicts with existing records or database constraints",
                request,
                null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You are not allowed to perform this action",
                request,
                null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        log.debug("Resource not found: {}", request.getRequestURI());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                errorCode,
                message,
                request.getRequestURI(),
                fieldErrors);

        return new ResponseEntity<>(error, status);
    }
}

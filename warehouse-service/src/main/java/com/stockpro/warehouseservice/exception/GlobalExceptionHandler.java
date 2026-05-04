package com.stockpro.warehouseservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WarehouseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            WarehouseNotFoundException ex, HttpServletRequest request) {
        log.error("WarehouseNotFoundException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(), "Not Found",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProductLookupException.class)
    public ResponseEntity<ErrorResponse> handleProductLookup(
            ProductLookupException ex, HttpServletRequest request) {
        log.error("ProductLookupException: {}", ex.getMessage());
        HttpStatus status = ex.isUpstreamFailure()
                ? HttpStatus.BAD_GATEWAY
                : HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        status.value(), status.getReasonPhrase(),
                        ex.getMessage(), request.getRequestURI()),
                status);
    }

    @ExceptionHandler({InsufficientStockException.class, StockNotAvailableException.class})
    public ResponseEntity<ErrorResponse> handleStockNotAvailable(
            RuntimeException ex, HttpServletRequest request) {
        log.error("StockNotAvailableException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CapacityExceededException.class)
    public ResponseEntity<ErrorResponse> handleCapacityExceeded(
            CapacityExceededException ex, HttpServletRequest request) {
        log.error("CapacityExceededException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(
            InvalidOperationException ex, HttpServletRequest request) {
        log.error("InvalidOperationException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        log.error("ConstraintViolationException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.error("IllegalArgumentException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.error("NoResourceFoundException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(), "Not Found",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("DataIntegrityViolationException: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.CONFLICT.value(), "Conflict",
                        "Duplicate resource or conflicting data",
                        request.getRequestURI()),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockConflict(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.error("Optimistic locking conflict: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.CONFLICT.value(), "Conflict",
                        "Stock was updated by another transaction. Please retry.",
                        request.getRequestURI()),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An unexpected error occurred",
                        request.getRequestURI()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

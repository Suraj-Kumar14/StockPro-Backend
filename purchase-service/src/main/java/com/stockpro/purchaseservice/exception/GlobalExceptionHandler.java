package com.stockpro.purchaseservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PurchaseOrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            PurchaseOrderNotFoundException ex, HttpServletRequest request) {
        log.error("PO not found: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(), "Not Found",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidPOStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatus(
            InvalidPOStatusException ex, HttpServletRequest request) {
        log.error("Invalid PO status: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidPurchaseOrderStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPurchaseOrderStatus(
            InvalidPurchaseOrderStatusException ex, HttpServletRequest request) {
        log.warn("Invalid receive status for request {}: {}", request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidReceiveQuantityException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReceiveQuantity(
            InvalidReceiveQuantityException ex, HttpServletRequest request) {
        log.warn("Invalid receive quantity for request {}: {}", request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WarehouseStockUpdateException.class)
    public ResponseEntity<ErrorResponse> handleWarehouseStockUpdate(
            WarehouseStockUpdateException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_GATEWAY;
        log.error("Warehouse stock update failed for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        status.value(), status.getReasonPhrase(),
                        ex.getMessage(), request.getRequestURI()),
                status);
    }

    @ExceptionHandler({
            InvalidPOStateException.class,
            InvalidLineItemException.class,
            OverReceiptException.class,
            SupplierNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleBusinessValidation(
            RuntimeException ex, HttpServletRequest request) {
        log.error("Business validation failed: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String parameterName = ex.getName() != null ? ex.getName() : "request parameter";
        String message = "Invalid value for " + parameterName;
        log.warn("Type mismatch on {}: {}", request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        message, request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for path {}: {}", request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.FORBIDDEN.value(), "Forbidden",
                        "Access is denied",
                        request.getRequestURI()),
                HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, JpaSystemException.class})
    public ResponseEntity<ErrorResponse> handlePersistenceFailure(
            RuntimeException ex, HttpServletRequest request) {
        log.error("Persistence failure on path {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        String message = "Purchase order data could not be saved. Please verify the database schema is up to date.";
        if (ex.getMessage() != null && ex.getMessage().contains("status")) {
            message = "Purchase order status could not be saved. Please update the purchase_orders.status column schema.";
        }
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        message,
                        request.getRequestURI()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.error("Concurrent update conflict: {}", ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.CONFLICT.value(), "Conflict",
                        "The purchase order was modified by another transaction. Please retry.",
                        request.getRequestURI()),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        // Log actuator paths at DEBUG level to avoid noisy logs
        if (path.startsWith("/actuator")) {
            log.debug("No static resource found for actuator path: {}", path);
        } else {
            log.warn("No resource found: {}", path);
        }
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(), "Not Found",
                        "Resource not found",
                        path),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on path {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An unexpected error occurred",
                        request.getRequestURI()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

}

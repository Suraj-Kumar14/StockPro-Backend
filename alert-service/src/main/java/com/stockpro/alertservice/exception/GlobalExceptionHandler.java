package com.stockpro.alertservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAlertNotFoundException(
            AlertNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Alert not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "ALERT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidAlertException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAlertException(
            InvalidAlertException ex,
            HttpServletRequest request) {
        log.warn("Invalid alert request: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors()
                .forEach(error -> errors.put(((FieldError) error).getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Validation failed", request, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        log.warn("Invalid request parameter {}={}", ex.getName(), ex.getValue());
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Invalid value for parameter '" + ex.getName() + "'", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingException(
            OptimisticLockingFailureException ex,
            HttpServletRequest request) {
        log.warn("Concurrent alert update conflict: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "CONFLICT", "The alert was modified by another transaction. Please retry.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        log.error("Alert persistence error: {}", ex.getMessage(), ex);
        return error(HttpStatus.BAD_REQUEST, "DATA_INTEGRITY_ERROR",
                "Unable to save alert because the request conflicts with database constraints.", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Malformed request body or invalid enum value", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Unauthorized", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You are not allowed to access this alert", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        log.debug("Resource not found: {}", ex.getResourcePath());
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String errorCode, String message, HttpServletRequest request) {
        return error(status, errorCode, message, request, null);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                        errorCode, message, request.getRequestURI(), fieldErrors),
                status);
    }
}

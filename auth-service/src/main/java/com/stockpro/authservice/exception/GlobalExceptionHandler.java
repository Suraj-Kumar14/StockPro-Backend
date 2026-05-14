package com.stockpro.authservice.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistsException ex) {
        log.warn("UserAlreadyExistsException: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", ex.getMessage(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("DataIntegrityViolationException: {}", ex.getMessage());
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        if (message != null && message.toLowerCase().contains("email")) {
            return build(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email already registered", null);
        }

        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "Duplicate or invalid data", null);
    }

    @ExceptionHandler({InvalidOtpException.class, EmailDeliveryException.class})
    public ResponseEntity<ErrorResponse> handleOtp(RuntimeException ex) {
        log.error("Auth flow exception: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "OTP_FLOW_ERROR", ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Invalid credentials: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), null);
    }

    @ExceptionHandler(InactiveAccountException.class)
    public ResponseEntity<ErrorResponse> handleInactiveAccount(InactiveAccountException ex) {
        log.warn("Inactive account access blocked: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "INACTIVE_ACCOUNT", ex.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(SelfDeactivationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleSelfDeactivation(SelfDeactivationNotAllowedException ex) {
        log.warn("User action blocked: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "ACTION_NOT_ALLOWED", ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", errors);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("Resource not found: {}", ex.getResourcePath());
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found", null);
    }

  
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("RuntimeException: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null);
    }

    // 4. Catch-all for unexpected errors → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Internal server error", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String errorCode, String message, Map<String, String> fieldErrors) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(), status.value(), errorCode, message, fieldErrors),
                status);
    }
}

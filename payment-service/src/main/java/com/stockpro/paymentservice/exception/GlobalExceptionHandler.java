package com.stockpro.paymentservice.exception;

import com.stockpro.paymentservice.enums.AlertSeverity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
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

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicatePaymentException ex, HttpServletRequest req) {
        log.warn("Duplicate payment attempt: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(PaymentLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleLimitExceeded(PaymentLimitExceededException ex, HttpServletRequest req) {
        log.warn("Payment amount exceeds configured limit: requestedAmount={} maxAllowedAmount={} path={}",
                ex.getRequestedAmount(), ex.getMaxAllowedAmount(), req.getRequestURI());
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("PAYMENT_LIMIT_EXCEEDED")
                .message(ex.getMessage())
                .requestedAmount(ex.getRequestedAmount())
                .maxAllowedAmount(ex.getMaxAllowedAmount())
                .remainingAmount(ex.getRemainingAmount())
                .splitAllowed(ex.isSplitAllowed())
                .path(req.getRequestURI())
                .build());
    }

    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentRequest(InvalidPaymentRequestException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(RazorpayIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleRazorpay(RazorpayIntegrationException ex, HttpServletRequest req) {
        log.error("Razorpay error: {}", ex.getMessage(), ex);
        return error(HttpStatus.BAD_GATEWAY, "RAZORPAY_INTEGRATION_ERROR", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternal(ExternalServiceException ex, HttpServletRequest req) {
        log.error("External service error: {}", ex.getMessage(), ex);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SERVICE_ERROR", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(PaymentValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(PaymentValidationException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, ex.getMessage(), req.getRequestURI());
    }

    /**
     * Handles cases where a path variable cannot be converted to the expected type.
     * Example: GET /api/v1/payments/summary — "summary" cannot convert to Long.
     * The regex constraint on /{paymentId:\d+} should prevent this, but this handler
     * provides a safety net and returns a clean 400 without exposing a stack trace.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String paramName = ex.getName();
        String valueProvided = ex.getValue() != null ? ex.getValue().toString() : "null";
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = "purchaseOrderId".equals(paramName)
                ? "Invalid or missing purchaseOrderId"
                : String.format("Invalid value '%s' for parameter '%s'. Expected type: %s.",
                valueProvided, paramName, expectedType);
        log.warn("[GlobalExceptionHandler] MethodArgumentTypeMismatch: param={} value={} expectedType={} path={}",
                paramName, valueProvided, expectedType, req.getRequestURI());
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, message, req.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You are not allowed to perform this action", req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> errors.put(((FieldError) e).getField(), e.getDefaultMessage()));
        String msg = errors.containsKey("purchaseOrderId")
                ? "Invalid or missing purchaseOrderId"
                : errors.values().stream().findFirst().orElse("Validation failed");
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(VALIDATION_ERROR)
                .message(msg)
                .fieldErrors(errors)
                .path(req.getRequestURI())
                .build());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest req) {
        log.warn("No resource found for path={}", req.getRequestURI());
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Requested resource not found", req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred", req.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String errorCode, String message, String path) {
        return new ResponseEntity<>(
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .errorCode(errorCode)
                        .message(message)
                        .path(path)
                        .build(),
                status);
    }

}

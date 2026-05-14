package com.stockpro.reportservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDataNotFound(
            DataNotFoundException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(), "REPORT_DATA_UNAVAILABLE",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ReportGenerationException.class)
    public ResponseEntity<ErrorResponse> handleReportGeneration(
            ReportGenerationException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.SERVICE_UNAVAILABLE.value(), "DOWNSTREAM_SERVICE_UNAVAILABLE",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegal(
            IllegalArgumentException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), VALIDATION_ERROR,
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

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), VALIDATION_ERROR,
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(), VALIDATION_ERROR,
                        ex.getParameterName() + " is required",
                        request.getRequestURI()),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.CONFLICT.value(), "Conflict",
                        "Snapshot data was modified concurrently. Please retry.",
                        request.getRequestURI()),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(), "Not Found",
                        ex.getMessage(), request.getRequestURI()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for path {}: {}", request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(LocalDateTime.now(),
                        HttpStatus.FORBIDDEN.value(), "Forbidden",
                        "You do not have permission to access this resource",
                        request.getRequestURI()),
                HttpStatus.FORBIDDEN);
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

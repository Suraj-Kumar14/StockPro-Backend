package com.stockpro.alert.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AlertNotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({
            InvalidAlertRequestException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        String message;
        if (exception instanceof MethodArgumentNotValidException validationException) {
            message = java.util.stream.Stream.concat(
                            validationException.getBindingResult().getFieldErrors().stream()
                                    .map(FieldError::getDefaultMessage),
                            validationException.getBindingResult().getGlobalErrors().stream()
                                    .map(error -> error.getDefaultMessage()))
                    .collect(Collectors.joining(" "));
        } else if (exception instanceof ConstraintViolationException constraintViolationException) {
            message = constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .collect(Collectors.joining(" "));
        } else {
            message = exception.getMessage();
        }

        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({
            UnauthorizedAlertAccessException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ErrorResponse> handleForbidden(Exception exception, HttpServletRequest request) {
        String message = exception instanceof UnauthorizedAlertAccessException
                ? exception.getMessage()
                : "You do not have permission to perform this action.";
        return buildResponse(HttpStatus.FORBIDDEN, message, request);
    }

    @ExceptionHandler(EmailDispatchException.class)
    public ResponseEntity<ErrorResponse> handleEmailDispatch(EmailDispatchException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(AlertEventProcessingException.class)
    public ResponseEntity<ErrorResponse> handleEventProcessing(AlertEventProcessingException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
}

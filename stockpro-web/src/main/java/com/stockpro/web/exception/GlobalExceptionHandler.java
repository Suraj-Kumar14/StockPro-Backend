package com.stockpro.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiClientException.class)
    public String handleApiClientException(ApiClientException exception, HttpServletRequest request, Model model) {
        populate(model,
                "Integration Error",
                exception.getMessage(),
                exception.getStatusCode(),
                request.getRequestURI());
        return "error/service-error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationException(MethodArgumentNotValidException exception,
            HttpServletRequest request, Model model) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed.");
        populate(model, "Validation Error", message, 400, request.getRequestURI());
        return "error/service-error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException exception,
            HttpServletRequest request, Model model) {
        populate(model,
                "Access Denied",
                "You do not have permission to access this page or perform this action.",
                403,
                request.getRequestURI());
        return "error/403";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception exception, HttpServletRequest request, Model model) {
        populate(model,
                "Unexpected Error",
                exception.getMessage() == null ? "An unexpected error occurred." : exception.getMessage(),
                500,
                request.getRequestURI());
        return "error/service-error";
    }

    private void populate(Model model, String title, String message, int statusCode, String path) {
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("requestPath", path);
    }
}

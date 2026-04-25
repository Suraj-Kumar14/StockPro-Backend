package com.stockpro.alert.validation;

import com.stockpro.alert.exception.AlertEventProcessingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AlertEventValidator {

    private final Validator validator;

    public AlertEventValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> void validate(T event) {
        Set<ConstraintViolation<T>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(" "));
            throw new AlertEventProcessingException(message);
        }
    }
}

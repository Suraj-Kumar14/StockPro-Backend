package com.stockpro.movement.validation;

import com.stockpro.movement.dto.request.MovementSearchRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, MovementSearchRequest> {

    @Override
    public boolean isValid(MovementSearchRequest value, ConstraintValidatorContext context) {
        if (value == null || value.getStartDate() == null || value.getEndDate() == null) {
            return true;
        }

        return !value.getEndDate().isBefore(value.getStartDate());
    }
}

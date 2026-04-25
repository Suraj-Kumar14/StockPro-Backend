package com.stockpro.alert.validation;

import com.stockpro.alert.dto.request.AlertSearchRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AlertSearchDateRangeValidator implements ConstraintValidator<ValidAlertSearchDateRange, AlertSearchRequest> {

    @Override
    public boolean isValid(AlertSearchRequest value, ConstraintValidatorContext context) {
        if (value == null || value.getStartDate() == null || value.getEndDate() == null) {
            return true;
        }

        return !value.getStartDate().isAfter(value.getEndDate());
    }
}

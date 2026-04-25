package com.stockpro.report.validation;

import com.stockpro.report.dto.request.GenerateReportRequest;
import com.stockpro.report.dto.request.ReportFilterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class ReportDateRangeValidator implements ConstraintValidator<ValidReportDateRange, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value instanceof ReportFilterRequest request) {
            return isDateRangeValid(request.getFromDate(), request.getToDate());
        }

        if (value instanceof GenerateReportRequest request) {
            return isDateRangeValid(request.getFromDate(), request.getToDate());
        }

        return true;
    }

    private boolean isDateRangeValid(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            return true;
        }

        return !toDate.isBefore(fromDate);
    }
}

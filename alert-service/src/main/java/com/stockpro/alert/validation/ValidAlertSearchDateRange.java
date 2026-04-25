package com.stockpro.alert.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AlertSearchDateRangeValidator.class)
public @interface ValidAlertSearchDateRange {

    String message() default "startDate must be before or equal to endDate.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

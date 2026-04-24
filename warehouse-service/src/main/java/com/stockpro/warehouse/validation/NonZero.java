package com.stockpro.warehouse.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NonZeroBigDecimalValidator.class)
public @interface NonZero {

    String message() default "value must be non-zero.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

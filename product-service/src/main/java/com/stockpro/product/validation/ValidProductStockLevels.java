package com.stockpro.product.validation;

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
@Constraint(validatedBy = ProductStockLevelsValidator.class)
public @interface ValidProductStockLevels {

    String message() default "maxStockLevel must be greater than or equal to reorderLevel.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

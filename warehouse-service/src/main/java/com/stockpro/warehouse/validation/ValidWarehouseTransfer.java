package com.stockpro.warehouse.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = WarehouseTransferRequestValidator.class)
public @interface ValidWarehouseTransfer {

    String message() default "Source warehouse must not equal destination warehouse.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

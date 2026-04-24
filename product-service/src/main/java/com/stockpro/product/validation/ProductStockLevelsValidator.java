package com.stockpro.product.validation;

import com.stockpro.product.dto.request.CreateProductRequest;
import com.stockpro.product.dto.request.UpdateProductRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class ProductStockLevelsValidator implements ConstraintValidator<ValidProductStockLevels, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        BigDecimal reorderLevel = null;
        BigDecimal maxStockLevel = null;

        if (value instanceof CreateProductRequest request) {
            reorderLevel = request.getReorderLevel();
            maxStockLevel = request.getMaxStockLevel();
        } else if (value instanceof UpdateProductRequest request) {
            reorderLevel = request.getReorderLevel();
            maxStockLevel = request.getMaxStockLevel();
        }

        if (reorderLevel == null || maxStockLevel == null) {
            return true;
        }

        return maxStockLevel.compareTo(reorderLevel) >= 0;
    }
}

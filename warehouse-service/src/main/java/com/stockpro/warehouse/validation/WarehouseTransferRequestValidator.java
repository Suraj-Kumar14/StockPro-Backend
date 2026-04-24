package com.stockpro.warehouse.validation;

import com.stockpro.warehouse.dto.request.TransferStockRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class WarehouseTransferRequestValidator implements ConstraintValidator<ValidWarehouseTransfer, TransferStockRequest> {

    @Override
    public boolean isValid(TransferStockRequest value, ConstraintValidatorContext context) {
        if (value == null || value.getSourceWarehouseId() == null || value.getDestinationWarehouseId() == null) {
            return true;
        }
        return !value.getSourceWarehouseId().equals(value.getDestinationWarehouseId());
    }
}

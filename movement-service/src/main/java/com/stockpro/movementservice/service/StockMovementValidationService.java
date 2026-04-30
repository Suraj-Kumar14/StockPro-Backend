package com.stockpro.movementservice.service;

import com.stockpro.movementservice.dto.StockMovementRequestDTO;
import com.stockpro.movementservice.entity.MovementType;
import com.stockpro.movementservice.exception.InvalidMovementException;
import org.springframework.stereotype.Component;

@Component
public class StockMovementValidationService {

    public void validateForRecord(StockMovementRequestDTO request) {
        if (request.getReferenceId() == null) {
            throw new InvalidMovementException("Reference ID is required");
        }
        if (request.getQuantity() == null || request.getQuantity() == 0) {
            throw new InvalidMovementException("Quantity must be non-zero");
        }
        if (request.getMovementType() != MovementType.ADJUSTMENT && request.getQuantity() < 0) {
            throw new InvalidMovementException("Negative quantity is only allowed for adjustments");
        }
        if (request.getMovementType() != MovementType.ADJUSTMENT && Math.abs(request.getQuantity()) < 1) {
            throw new InvalidMovementException("Quantity must be greater than zero");
        }

        switch (request.getMovementType()) {
            case STOCK_IN -> requireReferenceType(request, "PO");
            case STOCK_OUT -> validatePositiveQuantity(request, "STOCK_OUT quantity must be greater than zero");
            case TRANSFER_IN, TRANSFER_OUT -> requireReferenceType(request, "TRANSFER");
            case WRITE_OFF -> {
                validatePositiveQuantity(request, "WRITE_OFF quantity must be greater than zero");
                requireReason(request, "WRITE_OFF reason is required");
            }
            case RETURN -> requireReason(request, "RETURN notes must describe return context");
            case ADJUSTMENT -> requireReason(request, "ADJUSTMENT reason is required");
            default -> throw new InvalidMovementException("Unsupported movement type: " + request.getMovementType());
        }
    }

    public void validateBalanceAfter(int computedBalanceAfter, Integer requestedBalanceAfter) {
        if (requestedBalanceAfter != null && computedBalanceAfter != requestedBalanceAfter) {
            throw new InvalidMovementException(
                    "Provided balanceAfter does not match computed balance. Expected " + computedBalanceAfter);
        }
    }

    private void validatePositiveQuantity(StockMovementRequestDTO request, String message) {
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new InvalidMovementException(message);
        }
    }

    private void requireReferenceType(StockMovementRequestDTO request, String expectedReferenceType) {
        if (request.getReferenceType() == null || request.getReferenceType().trim().isEmpty()) {
            throw new InvalidMovementException("Reference type is required");
        }
        if (!expectedReferenceType.equalsIgnoreCase(request.getReferenceType().trim())) {
            throw new InvalidMovementException(
                    "Reference type must be " + expectedReferenceType + " for " + request.getMovementType());
        }
    }

    private void requireReason(StockMovementRequestDTO request, String message) {
        if (request.getNotes() == null || request.getNotes().trim().isEmpty()) {
            throw new InvalidMovementException(message);
        }
    }
}

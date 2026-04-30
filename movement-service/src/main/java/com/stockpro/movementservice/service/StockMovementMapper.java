package com.stockpro.movementservice.service;

import com.stockpro.movementservice.dto.StockMovementResponseDTO;
import com.stockpro.movementservice.entity.StockMovement;
import org.springframework.stereotype.Component;

@Component
public class StockMovementMapper {

    public StockMovementResponseDTO toResponse(StockMovement movement) {
        return StockMovementResponseDTO.builder()
                .movementId(movement.getMovementId())
                .productId(movement.getProductId())
                .warehouseId(movement.getWarehouseId())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .referenceId(movement.getReferenceId())
                .referenceType(movement.getReferenceType())
                .unitCost(movement.getUnitCost())
                .performedBy(movement.getPerformedBy())
                .notes(movement.getNotes())
                .movementDate(movement.getMovementDate())
                .balanceAfter(movement.getBalanceAfter())
                .build();
    }
}

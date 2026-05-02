package com.stockpro.movementservice.service;

import com.stockpro.movementservice.dto.response.MovementResponse;
import com.stockpro.movementservice.entity.StockMovement;
import org.springframework.stereotype.Component;

@Component
public class MovementMapper {

    public MovementResponse toResponse(StockMovement movement) {
        return new MovementResponse(
                movement.getMovementId(),
                movement.getMovementNumber(),
                movement.getProductId(),
                movement.getProductSku(),
                movement.getProductName(),
                movement.getWarehouseId(),
                movement.getWarehouseCode(),
                movement.getWarehouseName(),
                movement.getMovementType(),
                movement.getDirection(),
                movement.getQuantity(),
                movement.getUnitCost(),
                movement.getTotalValue(),
                movement.getBalanceAfter(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getReferenceNumber(),
                movement.getPerformedBy(),
                movement.getPerformedByName(),
                movement.getReasonCode(),
                movement.getNotes(),
                movement.getRelatedMovementId(),
                movement.getIsReversal(),
                movement.getMovementDate(),
                movement.getCreatedAt(),
                movement.getSourceService(),
                movement.getCorrelationId());
    }
}

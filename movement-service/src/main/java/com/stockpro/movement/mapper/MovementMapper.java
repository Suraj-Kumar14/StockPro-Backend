package com.stockpro.movement.mapper;

import com.stockpro.movement.dto.request.RecordMovementRequest;
import com.stockpro.movement.dto.response.MovementResponse;
import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

@Component
public class MovementMapper {

    public StockMovement toEntity(RecordMovementRequest request, AuthenticatedUser authenticatedUser, String sourceService) {
        return StockMovement.builder()
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .movementType(request.getMovementType())
                .quantity(request.getQuantity())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .unitCost(request.getUnitCost())
                .performedBy(authenticatedUser.userId())
                .notes(request.getNotes())
                .movementDate(request.getMovementDate())
                .balanceAfter(request.getBalanceAfter())
                .createdBy(authenticatedUser.email())
                .sourceService(sourceService)
                .build();
    }

    public MovementResponse toResponse(StockMovement movement) {
        return MovementResponse.builder()
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
                .createdAt(movement.getCreatedAt())
                .build();
    }
}

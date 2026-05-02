package com.stockpro.movementservice.dto.request;

import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementReasonCode;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateMovementFromEventRequest(
        String eventId,
        String eventType,
        Long productId,
        String productSku,
        String productName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long sourceWarehouseId,
        Long destinationWarehouseId,
        MovementType movementType,
        MovementDirection direction,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal balanceAfter,
        ReferenceType referenceType,
        String referenceId,
        String referenceNumber,
        Long performedBy,
        String performedByName,
        MovementReasonCode reasonCode,
        String notes,
        LocalDateTime eventTime,
        String sourceService,
        String correlationId) {
}

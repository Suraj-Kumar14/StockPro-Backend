package com.stockpro.movementservice.dto.response;

import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementReasonCode;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovementResponse(
        Long movementId,
        String movementNumber,
        Long productId,
        String productSku,
        String productName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        MovementType movementType,
        MovementDirection direction,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal totalValue,
        BigDecimal balanceAfter,
        ReferenceType referenceType,
        String referenceId,
        String referenceNumber,
        Long performedBy,
        String performedByName,
        MovementReasonCode reasonCode,
        String notes,
        Long relatedMovementId,
        Boolean isReversal,
        LocalDateTime movementDate,
        LocalDateTime createdAt,
        String sourceService,
        String correlationId) {
}

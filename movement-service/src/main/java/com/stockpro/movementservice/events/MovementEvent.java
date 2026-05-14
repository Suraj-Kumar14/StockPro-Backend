package com.stockpro.movementservice.events;

import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementEventType;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovementEvent(
        String eventId,
        MovementEventType eventType,
        Long movementId,
        String movementNumber,
        Long productId,
        Long warehouseId,
        MovementType movementType,
        MovementDirection direction,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal totalValue,
        BigDecimal balanceAfter,
        ReferenceType referenceType,
        String referenceId,
        Long performedBy,
        LocalDateTime movementDate,
        String sourceService,
        String correlationId) {
}

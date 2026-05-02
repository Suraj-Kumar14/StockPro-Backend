package com.stockpro.movementservice.dto.request;

import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record MovementSearchRequest(
        String keyword,
        Long productId,
        Long warehouseId,
        MovementType movementType,
        MovementDirection direction,
        ReferenceType referenceType,
        String referenceId,
        Long performedBy,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        BigDecimal minQuantity,
        BigDecimal maxQuantity,
        String sourceService,
        String correlationId,
        Boolean isReversal,
        Integer page,
        Integer size,
        String sortBy,
        String sortDir) {
}

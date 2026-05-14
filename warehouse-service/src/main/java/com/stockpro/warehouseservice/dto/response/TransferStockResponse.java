package com.stockpro.warehouseservice.dto.response;

import com.stockpro.warehouseservice.enums.TransferStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record TransferStockResponse(
        Long transferId,
        Long productId,
        Long sourceWarehouseId,
        Long destinationWarehouseId,
        Integer quantity,
        Integer sourceBalanceAfter,
        Integer destinationBalanceAfter,
        TransferStatus status,
        String message,
        LocalDateTime transferredAt) {
}

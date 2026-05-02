package com.stockpro.purchaseservice.dto.response;

import java.time.LocalDateTime;

public record PurchaseOrderHistoryResponse(
        Long historyId,
        String action,
        String oldStatus,
        String newStatus,
        Long actorId,
        String remarks,
        LocalDateTime actionAt) {
}

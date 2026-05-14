package com.stockpro.reportservice.dto.response;

import java.time.LocalDateTime;

public record PurchaseOrderTimelineItemResponse(
        String status,
        LocalDateTime changedAt,
        Long changedBy,
        String changedByName,
        String remarks) {
}

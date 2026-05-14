package com.stockpro.product_service.service;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record ProductAuditEntry(
        Long actorId,
        String action,
        String entityType,
        Long entityId,
        String oldValue,
        String newValue,
        LocalDateTime timestamp,
        String serviceName) {
}

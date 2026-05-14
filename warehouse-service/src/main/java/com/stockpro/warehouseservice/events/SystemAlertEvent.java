package com.stockpro.warehouseservice.events;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record SystemAlertEvent(
        String eventId,
        String eventType,
        String severity,
        String title,
        String message,
        List<String> recipientRoles,
        List<Long> recipientIds,
        String actionUrl,
        String referenceType,
        String referenceId,
        LocalDateTime expiresAt,
        String sourceService,
        String correlationId) {
}

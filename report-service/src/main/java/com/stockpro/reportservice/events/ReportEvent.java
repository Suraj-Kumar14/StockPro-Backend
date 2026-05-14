package com.stockpro.reportservice.events;

import com.stockpro.reportservice.enums.ReportType;
import java.time.LocalDateTime;
import java.util.Map;

public record ReportEvent(
        String eventId,
        ReportEventType eventType,
        ReportType reportType,
        LocalDateTime occurredAt,
        String status,
        Map<String, Object> metadata) {
}

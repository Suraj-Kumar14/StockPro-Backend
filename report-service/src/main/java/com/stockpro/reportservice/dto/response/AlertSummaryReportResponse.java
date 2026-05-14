package com.stockpro.reportservice.dto.response;

import java.util.Map;

public record AlertSummaryReportResponse(
        long totalAlerts,
        long unreadAlerts,
        long criticalAlerts,
        long warningAlerts,
        Map<String, Long> alertsByType) {
}

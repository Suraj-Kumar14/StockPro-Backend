package com.stockpro.reportservice.dto.response;

public record DashboardAlertItem(
        Long alertId,
        String title,
        String severity,
        String type,
        String createdAt) {
}

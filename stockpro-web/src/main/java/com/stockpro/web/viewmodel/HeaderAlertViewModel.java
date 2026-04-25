package com.stockpro.web.viewmodel;

public record HeaderAlertViewModel(
        Long alertId,
        String title,
        String message,
        String severity,
        String createdAtLabel,
        boolean unread) {
}

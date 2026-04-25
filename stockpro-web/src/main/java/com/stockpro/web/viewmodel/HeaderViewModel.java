package com.stockpro.web.viewmodel;

import java.util.List;

public record HeaderViewModel(
        String fullName,
        String email,
        String role,
        Long userId,
        long unreadCount,
        List<HeaderAlertViewModel> previewAlerts) {
}

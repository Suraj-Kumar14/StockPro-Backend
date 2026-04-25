package com.stockpro.web.viewmodel;

import java.util.List;

public record AdminDashboardViewModel(
        List<MetricCardViewModel> metrics,
        List<QuickActionViewModel> quickActions,
        List<AuditEntryViewModel> recentEvents) {
}

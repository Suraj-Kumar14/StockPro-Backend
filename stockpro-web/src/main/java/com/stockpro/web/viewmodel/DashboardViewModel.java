package com.stockpro.web.viewmodel;

import java.util.List;

public record DashboardViewModel(
        List<MetricCardViewModel> metrics,
        List<QuickActionViewModel> quickActions) {
}

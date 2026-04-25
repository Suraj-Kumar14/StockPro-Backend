package com.stockpro.web.viewmodel;

public record MetricCardViewModel(
        String title,
        String value,
        String tone,
        String helperText,
        String href) {
}

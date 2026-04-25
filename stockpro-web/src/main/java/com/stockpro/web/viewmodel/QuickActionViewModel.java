package com.stockpro.web.viewmodel;

public record QuickActionViewModel(
        String title,
        String description,
        String href,
        String icon,
        String tone) {
}

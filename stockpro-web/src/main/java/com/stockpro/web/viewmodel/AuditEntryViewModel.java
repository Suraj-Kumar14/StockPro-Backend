package com.stockpro.web.viewmodel;

import java.time.LocalDateTime;

public record AuditEntryViewModel(
        String source,
        String title,
        String detail,
        LocalDateTime timestamp,
        String tone) {
}

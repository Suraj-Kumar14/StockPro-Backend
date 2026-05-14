package com.stockpro.authservice.service;

import java.time.LocalDateTime;

public record OtpNotificationEvent(
        String eventId,
        String email,
        String otpCode,
        String purpose,
        String recipientName,
        LocalDateTime expiresAt,
        LocalDateTime requestedAt) {
}

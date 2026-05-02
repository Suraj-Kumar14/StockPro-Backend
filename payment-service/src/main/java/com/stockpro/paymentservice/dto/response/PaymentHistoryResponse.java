package com.stockpro.paymentservice.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record PaymentHistoryResponse(
        Long historyId,
        Long paymentId,
        String action,
        String oldStatus,
        String newStatus,
        Long actorId,
        String remarks,
        LocalDateTime actionAt) {
}

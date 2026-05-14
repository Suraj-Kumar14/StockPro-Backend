package com.stockpro.paymentservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.math.BigDecimal;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String errorCode,
        String message,
        Map<String, String> fieldErrors,
        BigDecimal requestedAmount,
        BigDecimal maxAllowedAmount,
        BigDecimal remainingAmount,
        Boolean splitAllowed,
        String path) {
}

package com.stockpro.movementservice.dto.request;

import com.stockpro.movementservice.enums.MovementReasonCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReverseMovementRequest(
        @NotNull(message = "Reason code is required") MovementReasonCode reasonCode,
        @Size(max = 1000, message = "Notes must be less than or equal to 1000 characters") String notes) {
}

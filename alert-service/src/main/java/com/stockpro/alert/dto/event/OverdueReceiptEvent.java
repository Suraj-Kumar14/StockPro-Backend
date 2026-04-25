package com.stockpro.alert.dto.event;

import com.stockpro.alert.enums.AlertSeverity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverdueReceiptEvent {

    @NotNull(message = "recipientId is required.")
    @Positive(message = "recipientId must be greater than zero.")
    private Long recipientId;

    @NotNull(message = "purchaseOrderId is required.")
    @Positive(message = "purchaseOrderId must be greater than zero.")
    private Long purchaseOrderId;

    @NotNull(message = "supplierId is required.")
    @Positive(message = "supplierId must be greater than zero.")
    private Long supplierId;

    @NotNull(message = "warehouseId is required.")
    @Positive(message = "warehouseId must be greater than zero.")
    private Long warehouseId;

    @NotNull(message = "expectedDate is required.")
    private LocalDate expectedDate;

    @NotNull(message = "severity is required.")
    private AlertSeverity severity;

    @NotNull(message = "eventTime is required.")
    private LocalDateTime eventTime;
}

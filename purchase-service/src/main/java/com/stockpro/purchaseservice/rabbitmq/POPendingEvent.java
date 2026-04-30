package com.stockpro.purchaseservice.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POPendingEvent {

    private Long poId;
    private Long supplierId;
    private Long warehouseId;
    private Long requestedByUserId;
    private LocalDate expectedDate;
    private LocalDateTime submittedAt;
}

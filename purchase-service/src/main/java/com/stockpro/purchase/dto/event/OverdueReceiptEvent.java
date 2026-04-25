package com.stockpro.purchase.dto.event;

import com.stockpro.purchase.enums.AlertSeverity;
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

    private Long recipientId;
    private Long purchaseOrderId;
    private Long supplierId;
    private Long warehouseId;
    private LocalDate expectedDate;
    private AlertSeverity severity;
    private LocalDateTime eventTime;
}

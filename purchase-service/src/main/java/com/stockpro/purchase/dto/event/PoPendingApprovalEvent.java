package com.stockpro.purchase.dto.event;

import com.stockpro.purchase.enums.AlertSeverity;
import java.math.BigDecimal;
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
public class PoPendingApprovalEvent {

    private Long recipientId;
    private Long purchaseOrderId;
    private Long supplierId;
    private Long warehouseId;
    private String poNumber;
    private BigDecimal totalAmount;
    private AlertSeverity severity;
    private LocalDateTime eventTime;
}

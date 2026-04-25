package com.stockpro.report.client;

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
public class MovementClientResponse {

    private Long movementId;
    private Long productId;
    private Long warehouseId;
    private MovementTypeClient movementType;
    private BigDecimal quantity;
    private Long referenceId;
    private String referenceType;
    private BigDecimal unitCost;
    private Long performedBy;
    private String notes;
    private LocalDateTime movementDate;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
}

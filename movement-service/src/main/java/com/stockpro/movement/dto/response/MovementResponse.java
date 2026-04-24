package com.stockpro.movement.dto.response;

import com.stockpro.movement.enums.MovementType;
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
public class MovementResponse {

    private Long movementId;
    private Long productId;
    private Long warehouseId;
    private MovementType movementType;
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

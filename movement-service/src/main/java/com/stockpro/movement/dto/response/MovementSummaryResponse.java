package com.stockpro.movement.dto.response;

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
public class MovementSummaryResponse {

    private Long productId;
    private Long warehouseId;
    private Long totalMovements;
    private BigDecimal totalQuantityIn;
    private BigDecimal totalQuantityOut;
    private BigDecimal latestBalanceAfter;
    private LocalDateTime firstMovementDate;
    private LocalDateTime lastMovementDate;
}

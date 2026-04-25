package com.stockpro.report.dto.response;

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
public class DeadStockResponse {

    private Long productId;
    private String productName;
    private Long warehouseId;
    private LocalDateTime lastMovementDate;
    private Long daysWithoutMovement;
}

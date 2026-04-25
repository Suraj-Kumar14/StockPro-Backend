package com.stockpro.report.dto.response;

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
public class SlowMovingProductResponse {

    private Long productId;
    private String productName;
    private BigDecimal totalMovementQuantity;
    private LocalDateTime lastMovementDate;
}

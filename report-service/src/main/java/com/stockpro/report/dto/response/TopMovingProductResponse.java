package com.stockpro.report.dto.response;

import java.math.BigDecimal;
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
public class TopMovingProductResponse {

    private Long productId;
    private String productName;
    private BigDecimal totalMovementQuantity;
    private Integer rank;
}

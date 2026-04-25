package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopMovingProductResponse {

    private Long productId;
    private String productName;
    private BigDecimal totalMovementQuantity;
    private Integer rank;
}

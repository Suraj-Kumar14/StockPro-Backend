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
public class TransferStockResponse {

    private Long productId;
    private Long sourceWarehouseId;
    private Long destinationWarehouseId;
    private BigDecimal transferredQuantity;
    private BigDecimal sourceBalance;
    private BigDecimal destinationBalance;
    private String message;
}

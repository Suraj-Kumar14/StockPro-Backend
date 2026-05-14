package com.stockpro.warehouseservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAuditResponseDTO {

    private Long warehouseId;
    private Long productId;
    private Integer systemQuantity;
    private Integer countedQuantity;
    private Integer discrepancy;
    private String reason;
    private StockLevelResponseDTO updatedStock;
}

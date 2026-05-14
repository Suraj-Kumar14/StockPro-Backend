package com.stockpro.product_service.dto;

import lombok.Data;

@Data
public class WarehouseStockSnapshotDTO {

    private Long stockId;
    private Long warehouseId;
    private Long productId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
}

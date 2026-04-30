package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.StockProductThresholdDTO;

public interface WarehouseGateway {

    void ensureWarehouseExists(Long warehouseId);

    void increaseStock(Long warehouseId, Long productId, Integer quantity,
            StockProductThresholdDTO thresholds);
}

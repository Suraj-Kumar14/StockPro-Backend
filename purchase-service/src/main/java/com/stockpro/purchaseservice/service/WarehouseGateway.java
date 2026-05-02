package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.StockProductThresholdDTO;
import com.stockpro.purchaseservice.dto.WarehouseLookupResponseDTO;

public interface WarehouseGateway {

    void ensureWarehouseExists(Long warehouseId);

    void increaseStock(Long warehouseId, Long productId, Integer quantity,
            StockProductThresholdDTO thresholds);

    WarehouseLookupResponseDTO getWarehouse(Long warehouseId);
}

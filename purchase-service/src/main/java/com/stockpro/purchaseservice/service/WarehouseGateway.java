package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.StockProductThresholdDTO;
import com.stockpro.purchaseservice.dto.WarehouseLookupResponseDTO;
import java.math.BigDecimal;

public interface WarehouseGateway {

    void ensureWarehouseExists(Long warehouseId);

    void increaseStock(Long warehouseId, Long productId, Integer quantity, Long purchaseOrderId,
            String poNumber, BigDecimal unitCost, String notes, StockProductThresholdDTO thresholds);

    WarehouseLookupResponseDTO getWarehouse(Long warehouseId);
}

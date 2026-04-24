package com.stockpro.warehouse.mapper;

import com.stockpro.warehouse.dto.response.LowStockItemResponse;
import com.stockpro.warehouse.dto.response.StockLevelQuantityResponse;
import com.stockpro.warehouse.dto.response.StockLevelResponse;
import com.stockpro.warehouse.entity.StockLevel;
import org.springframework.stereotype.Component;

@Component
public class StockLevelMapper {

    public StockLevelResponse toResponse(StockLevel stockLevel) {
        return StockLevelResponse.builder()
                .stockId(stockLevel.getStockId())
                .warehouseId(stockLevel.getWarehouseId())
                .productId(stockLevel.getProductId())
                .quantity(stockLevel.getQuantity())
                .reservedQuantity(stockLevel.getReservedQuantity())
                .availableQuantity(stockLevel.getAvailableQuantity())
                .location(stockLevel.getLocation())
                .lastUpdated(stockLevel.getLastUpdated())
                .build();
    }

    public LowStockItemResponse toLowStockResponse(StockLevel stockLevel) {
        return LowStockItemResponse.builder()
                .warehouseId(stockLevel.getWarehouseId())
                .productId(stockLevel.getProductId())
                .quantity(stockLevel.getQuantity())
                .reservedQuantity(stockLevel.getReservedQuantity())
                .availableQuantity(stockLevel.getAvailableQuantity())
                .location(stockLevel.getLocation())
                .build();
    }

    public StockLevelQuantityResponse toQuantityResponse(Long productId, Integer currentQuantity) {
        return StockLevelQuantityResponse.builder()
                .productId(productId)
                .currentQuantity(currentQuantity)
                .build();
    }
}

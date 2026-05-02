package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record InventoryValuationResponse(
        BigDecimal totalInventoryValue,
        BigDecimal totalQuantity,
        long totalProducts,
        long totalWarehouses,
        List<WarehouseValuationItem> valuationByWarehouse,
        Map<String, BigDecimal> valuationByCategory,
        List<ProductValuationItem> valuationByProduct) {
}

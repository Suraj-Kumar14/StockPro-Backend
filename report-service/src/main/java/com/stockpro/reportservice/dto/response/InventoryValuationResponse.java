package com.stockpro.reportservice.dto.response;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public record InventoryValuationResponse(
        LocalDate asOfDate,
        BigDecimal totalInventoryValue,
        BigDecimal totalQuantity,
        long totalProducts,
        long totalWarehouses,
        List<WarehouseValuationItem> warehouseBreakdown,
        List<ProductValuationItem> productBreakdown,
        List<String> warnings) {
}

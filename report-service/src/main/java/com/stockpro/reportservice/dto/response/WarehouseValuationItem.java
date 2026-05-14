package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;

public record WarehouseValuationItem(
        Long warehouseId,
        String warehouseName,
        BigDecimal totalQuantity,
        BigDecimal stockValue) {
}

package com.stockpro.report.repository;

import java.math.BigDecimal;

public interface WarehouseStockValueProjection {

    Long getWarehouseId();

    String getWarehouseName();

    BigDecimal getTotalStockValue();
}

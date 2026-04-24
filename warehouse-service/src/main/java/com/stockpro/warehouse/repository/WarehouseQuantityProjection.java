package com.stockpro.warehouse.repository;

import java.math.BigDecimal;

public interface WarehouseQuantityProjection {

    Long getWarehouseId();

    BigDecimal getTotalQuantity();
}

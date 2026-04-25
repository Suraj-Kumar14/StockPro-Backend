package com.stockpro.report.repository;

import java.math.BigDecimal;

public interface ProductAverageStockValueProjection {

    Long getProductId();

    BigDecimal getAverageStockValue();
}

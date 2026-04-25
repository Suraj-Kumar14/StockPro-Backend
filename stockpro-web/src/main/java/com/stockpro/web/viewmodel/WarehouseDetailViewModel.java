package com.stockpro.web.viewmodel;

import com.stockpro.web.dto.response.ProductResponse;
import com.stockpro.web.dto.response.StockLevelResponse;
import com.stockpro.web.dto.response.WarehouseResponse;
import com.stockpro.web.dto.response.WarehouseUtilizationResponse;
import java.util.Map;

public record WarehouseDetailViewModel(
        WarehouseResponse warehouse,
        WarehouseUtilizationResponse utilization,
        PageViewModel<StockLevelResponse> stockLevels,
        Map<Long, ProductResponse> productsById) {
}

package com.stockpro.web.viewmodel;

import com.stockpro.web.dto.response.ProductResponse;
import com.stockpro.web.dto.response.StockLevelResponse;
import com.stockpro.web.dto.response.WarehouseResponse;
import java.util.Map;

public record ProductDetailViewModel(
        ProductResponse product,
        PageViewModel<StockLevelResponse> stockLevels,
        Map<Long, WarehouseResponse> warehousesById) {
}

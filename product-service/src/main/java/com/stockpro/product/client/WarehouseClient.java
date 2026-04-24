package com.stockpro.product.client;

import com.stockpro.product.dto.StockLevelResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client used to ask warehouse-service for live stock quantities.
 * The warehouse-service should expose a matching endpoint for this contract.
 */
@FeignClient(name = "${warehouse.service.name:WAREHOUSE-SERVICE}", path = "/warehouse/internal")
public interface WarehouseClient {

    @PostMapping("/stock-levels")
    List<StockLevelResponse> getStockLevels(@RequestBody List<Long> productIds);
}

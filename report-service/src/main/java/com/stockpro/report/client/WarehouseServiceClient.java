package com.stockpro.report.client;

import com.stockpro.report.config.FeignAuthForwardingConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "${warehouse.service.name:WAREHOUSE-SERVICE}",
        configuration = FeignAuthForwardingConfig.class)
public interface WarehouseServiceClient {

    @PostMapping("/api/v1/stock/search")
    PageResponse<WarehouseStockLevelClientResponse> searchStock(@RequestBody StockSearchClientRequest request);

    @GetMapping("/api/v1/stock/warehouse/{warehouseId}")
    PageResponse<WarehouseStockLevelClientResponse> getStockByWarehouse(@PathVariable Long warehouseId,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String sortBy,
            @RequestParam String sortDir);

    @GetMapping("/api/v1/stock/low-stock")
    PageResponse<LowStockItemClientResponse> getLowStockItems(@RequestParam int page, @RequestParam int size);

    @GetMapping("/api/v1/warehouses/{warehouseId}")
    WarehouseClientResponse getWarehouseById(@PathVariable("warehouseId") Long warehouseId);
}

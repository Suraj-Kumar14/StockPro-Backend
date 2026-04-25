package com.stockpro.purchase.client;

import com.stockpro.purchase.config.FeignAuthForwardingConfig;
import com.stockpro.purchase.dto.WarehouseSummaryResponse;
import com.stockpro.purchase.dto.WarehouseStockUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "${warehouse.service.name:WAREHOUSE-SERVICE}",
        configuration = FeignAuthForwardingConfig.class)
public interface WarehouseClient {

    @PutMapping("/api/v1/stock/update")
    void updateStock(@RequestBody WarehouseStockUpdateRequest request);

    @GetMapping("/api/v1/warehouses/{warehouseId}")
    WarehouseSummaryResponse getWarehouseById(@PathVariable Long warehouseId);
}

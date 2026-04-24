package com.stockpro.purchase.client;

import com.stockpro.purchase.config.FeignAuthForwardingConfig;
import com.stockpro.purchase.dto.WarehouseStockUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "${warehouse.service.name:WAREHOUSE-SERVICE}",
        configuration = FeignAuthForwardingConfig.class,
        path = "/api/v1/stock")
public interface WarehouseClient {

    @PutMapping("/{warehouseId}/products/{productId}")
    void updateStock(@PathVariable Long warehouseId,
            @PathVariable Long productId,
            @RequestBody WarehouseStockUpdateRequest request);
}

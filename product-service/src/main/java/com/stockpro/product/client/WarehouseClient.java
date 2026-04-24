package com.stockpro.product.client;

import com.stockpro.product.config.FeignAuthForwardingConfig;
import com.stockpro.product.dto.response.StockLevelQuantityResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "${warehouse.service.name:WAREHOUSE-SERVICE}",
        configuration = FeignAuthForwardingConfig.class,
        path = "${warehouse.service.internal-path:/warehouse/internal}")
public interface WarehouseClient {

    @PostMapping("/stock-levels")
    List<StockLevelQuantityResponse> getStockLevels(@RequestBody List<Long> productIds);
}

package com.stockpro.web.client;

import com.stockpro.web.config.FeignSupportConfig;
import com.stockpro.web.dto.request.StockSearchRequest;
import com.stockpro.web.dto.request.TransferStockRequest;
import com.stockpro.web.dto.request.WarehouseFormRequest;
import com.stockpro.web.dto.response.ApiPageResponse;
import com.stockpro.web.dto.response.LowStockItemResponse;
import com.stockpro.web.dto.response.StockLevelResponse;
import com.stockpro.web.dto.response.TransferStockResponse;
import com.stockpro.web.dto.response.WarehouseResponse;
import com.stockpro.web.dto.response.WarehouseUtilizationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "warehouseServiceClient",
        url = "${api.gateway.base-url}",
        configuration = FeignSupportConfig.class)
public interface WarehouseServiceClient {

    @GetMapping("/api/v1/warehouses")
    ApiPageResponse<WarehouseResponse> getWarehouses(@RequestParam int page,
            @RequestParam int size,
            @RequestParam String sortBy,
            @RequestParam String sortDir);

    @GetMapping("/api/v1/warehouses/{warehouseId}")
    WarehouseResponse getWarehouseById(@PathVariable("warehouseId") Long warehouseId);

    @GetMapping("/api/v1/warehouses/{warehouseId}/utilization")
    WarehouseUtilizationResponse getUtilization(@PathVariable("warehouseId") Long warehouseId);

    @PostMapping("/api/v1/warehouses")
    WarehouseResponse createWarehouse(@RequestBody WarehouseFormRequest request);

    @PutMapping("/api/v1/warehouses/{warehouseId}")
    WarehouseResponse updateWarehouse(@PathVariable("warehouseId") Long warehouseId,
            @RequestBody WarehouseFormRequest request);

    @PutMapping("/api/v1/warehouses/{warehouseId}/deactivate")
    WarehouseResponse deactivateWarehouse(@PathVariable("warehouseId") Long warehouseId);

    @GetMapping("/api/v1/stock/level")
    StockLevelResponse getStockLevel(@RequestParam Long warehouseId, @RequestParam Long productId);

    @GetMapping("/api/v1/stock/warehouse/{warehouseId}")
    ApiPageResponse<StockLevelResponse> getStockByWarehouse(@PathVariable("warehouseId") Long warehouseId,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String sortBy,
            @RequestParam String sortDir);

    @GetMapping("/api/v1/stock/product/{productId}")
    ApiPageResponse<StockLevelResponse> getStockByProduct(@PathVariable("productId") Long productId,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam String sortBy,
            @RequestParam String sortDir);

    @PostMapping("/api/v1/stock/search")
    ApiPageResponse<StockLevelResponse> searchStock(@RequestBody StockSearchRequest request);

    @GetMapping("/api/v1/stock/low-stock")
    ApiPageResponse<LowStockItemResponse> getLowStockItems(@RequestParam int page, @RequestParam int size);

    @PostMapping("/api/v1/stock/transfer")
    TransferStockResponse transferStock(@RequestBody TransferStockRequest request);
}

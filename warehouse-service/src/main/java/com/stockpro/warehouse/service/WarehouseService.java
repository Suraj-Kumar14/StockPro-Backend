package com.stockpro.warehouse.service;

import com.stockpro.warehouse.dto.request.CreateWarehouseRequest;
import com.stockpro.warehouse.dto.request.ReleaseReservationRequest;
import com.stockpro.warehouse.dto.request.ReserveStockRequest;
import com.stockpro.warehouse.dto.request.StockSearchRequest;
import com.stockpro.warehouse.dto.request.TransferStockRequest;
import com.stockpro.warehouse.dto.request.UpdateStockRequest;
import com.stockpro.warehouse.dto.request.UpdateWarehouseRequest;
import com.stockpro.warehouse.dto.response.LowStockItemResponse;
import com.stockpro.warehouse.dto.response.StockLevelQuantityResponse;
import com.stockpro.warehouse.dto.response.StockLevelResponse;
import com.stockpro.warehouse.dto.response.TransferStockResponse;
import com.stockpro.warehouse.dto.response.WarehouseResponse;
import com.stockpro.warehouse.dto.response.WarehouseUtilizationResponse;
import java.util.List;
import org.springframework.data.domain.Page;

public interface WarehouseService {

    WarehouseResponse createWarehouse(CreateWarehouseRequest request);

    WarehouseResponse getById(Long warehouseId);

    Page<WarehouseResponse> getAllWarehouses(int page, int size, String sortBy, String sortDir);

    WarehouseResponse updateWarehouse(Long warehouseId, UpdateWarehouseRequest request);

    WarehouseResponse deactivateWarehouse(Long warehouseId);

    StockLevelResponse getStockLevel(Long warehouseId, Long productId);

    StockLevelResponse updateStock(UpdateStockRequest request);

    StockLevelResponse reserveStock(ReserveStockRequest request);

    StockLevelResponse releaseReservation(ReleaseReservationRequest request);

    TransferStockResponse transferStock(TransferStockRequest request);

    Page<StockLevelResponse> getStockByWarehouse(Long warehouseId, int page, int size, String sortBy, String sortDir);

    Page<StockLevelResponse> getStockByProduct(Long productId, int page, int size, String sortBy, String sortDir);

    Page<LowStockItemResponse> getLowStockItems(int page, int size);

    WarehouseUtilizationResponse getWarehouseUtilization(Long warehouseId);

    Page<StockLevelResponse> searchStock(StockSearchRequest request);

    List<LowStockItemResponse> getLowStockItemsForAlert();

    List<StockLevelQuantityResponse> getStockLevels(List<Long> productIds);
}

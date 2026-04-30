package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.dto.StockAuditRequestDTO;
import com.stockpro.warehouseservice.dto.StockAuditResponseDTO;
import com.stockpro.warehouseservice.dto.StockLevelResponseDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.Warehouse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockAuditService {

    private final StockMovementService stockMovementService;
    private final StockAlertService stockAlertService;
    private final InventoryOperationService inventoryOperationService;

    @Transactional
    public StockAuditResponseDTO performAudit(StockAuditRequestDTO dto) {
        Warehouse warehouse = inventoryOperationService.getWarehouseForMutation(
                dto.getWarehouseId());
        StockLevel stock = inventoryOperationService.getOrCreateStockLevel(
                dto.getWarehouseId(), dto.getProductId());
        InventoryOperationService.ThresholdSettings thresholds =
                inventoryOperationService.resolveThresholds(
                        dto.getProductId(), dto.getReorderLevel(), dto.getMaxStockLevel());

        inventoryOperationService.applyThresholds(stock, thresholds);
        inventoryOperationService.applyBinLocation(stock, dto.getBinLocation());

        int systemQuantity = stock.getQuantity();
        InventoryOperationService.StockMutation mutation =
                // Existing audit endpoint keeps its contract, but now reuses the same adjustment rules.
                inventoryOperationService.handleAdjustment(
                        warehouse, stock, dto.getCountedQuantity(), dto.getReason());

        StockLevel savedStock = inventoryOperationService.saveStockLevel(stock);
        inventoryOperationService.saveWarehouse(warehouse);

        stockMovementService.recordAudit(
                savedStock.getWarehouseId(),
                savedStock.getProductId(),
                mutation.quantityChanged(),
                mutation.previousQuantity(),
                mutation.newQuantity(),
                dto.getReason());
        stockAlertService.syncAlerts(
                savedStock, savedStock.getReorderLevel(), savedStock.getMaxStockLevel());

        log.info("Completed stock audit for warehouse {} and product {}",
                dto.getWarehouseId(), dto.getProductId());

        return StockAuditResponseDTO.builder()
                .warehouseId(savedStock.getWarehouseId())
                .productId(savedStock.getProductId())
                .systemQuantity(systemQuantity)
                .countedQuantity(savedStock.getQuantity())
                .discrepancy(savedStock.getQuantity() - systemQuantity)
                .reason(dto.getReason())
                .updatedStock(mapToDto(savedStock))
                .build();
    }

    private StockLevelResponseDTO mapToDto(StockLevel stock) {
        return StockLevelResponseDTO.builder()
                .stockId(stock.getStockId())
                .warehouseId(stock.getWarehouseId())
                .productId(stock.getProductId())
                .quantity(stock.getQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .availableQuantity(stock.getAvailableQuantity())
                .binLocation(stock.getBinLocation())
                .lastUpdated(stock.getLastUpdated())
                .build();
    }
}

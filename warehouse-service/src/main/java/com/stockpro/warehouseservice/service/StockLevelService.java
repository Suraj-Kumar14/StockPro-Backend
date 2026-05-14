package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.dto.StockLevelResponseDTO;
import com.stockpro.warehouseservice.dto.StockTransferDTO;
import com.stockpro.warehouseservice.dto.StockUpdateDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.exception.InvalidOperationException;
import com.stockpro.warehouseservice.exception.StockNotAvailableException;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.rabbitmq.StockEventPublisher;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockLevelService {

    private final StockLevelRepository stockLevelRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockEventPublisher stockEventPublisher;
    private final StockMovementService stockMovementService;
    private final StockAlertService stockAlertService;
    private final InventoryOperationService inventoryOperationService;

    public StockLevelResponseDTO getStockLevel(Long warehouseId, Long productId) {
        log.info("Fetching stock for product {} in warehouse {}", productId, warehouseId);
        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Stock not found for product " + productId
                                + " in warehouse " + warehouseId));
        return mapToDTO(stock);
    }

    public List<StockLevelResponseDTO> getStockByWarehouse(Long warehouseId) {
        log.info("Fetching all stock for warehouse: {}", warehouseId);
        validateWarehouseExists(warehouseId);
        return stockLevelRepository.findByWarehouseId(warehouseId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<StockLevelResponseDTO> getStockByProduct(Long productId) {
        log.info("Fetching stock across all warehouses for product: {}", productId);
        return stockLevelRepository.findByProductId(productId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional
    public StockLevelResponseDTO updateStock(Long warehouseId, StockUpdateDTO dto) {
        log.info("Processing stock update for product {} in warehouse {}",
                dto.getProductId(), warehouseId);

        Warehouse warehouse = inventoryOperationService.getWarehouseForMutation(warehouseId);
        StockLevel stock = inventoryOperationService.getOrCreateStockLevel(
                warehouseId, dto.getProductId());
        InventoryOperationService.ThresholdSettings thresholds =
                inventoryOperationService.resolveThresholds(
                        dto.getProductId(), dto.getReorderLevel(), dto.getMaxStockLevel());

        inventoryOperationService.applyThresholds(stock, thresholds);
        inventoryOperationService.applyBinLocation(stock, dto.getBinLocation());

        // Existing PUT endpoint now maps quantity deltas to receipt, issue, or no-op adjustment.
        InventoryOperationService.StockMutation mutation =
                inventoryOperationService.handleAdjustment(warehouse, stock, dto.getQuantity(), null);

        StockLevel savedStock = inventoryOperationService.saveStockLevel(stock);
        inventoryOperationService.saveWarehouse(warehouse);

        recordStockUpdateMovement(savedStock, mutation);
        syncStockSignals(savedStock);

        log.info("Completed stock update: operationType={}, warehouseId={}, productId={}",
                mutation.operationType(), savedStock.getWarehouseId(), savedStock.getProductId());
        return mapToDTO(savedStock);
    }

    @Transactional
    public void reserveStock(Long warehouseId, Long productId, Integer quantity) {
        log.info("Reserving stock: warehouseId={}, productId={}, operationType=RESERVATION",
                warehouseId, productId);
        validateWarehouseExists(warehouseId);

        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new StockNotAvailableException(
                        "Stock not found for product " + productId
                                + " in warehouse " + warehouseId));

        InventoryOperationService.ReservationMutation mutation =
                inventoryOperationService.reserveStock(stock, quantity);
        StockLevel savedStock = inventoryOperationService.saveStockLevel(stock);

        stockMovementService.recordReservation(
                savedStock.getWarehouseId(),
                savedStock.getProductId(),
                mutation.quantityChanged(),
                mutation.previousReservedQuantity(),
                mutation.newReservedQuantity(),
                "Stock reserved");
        syncStockSignals(savedStock);

        log.info("Completed stock reservation: warehouseId={}, productId={}, operationType=RESERVATION",
                savedStock.getWarehouseId(), savedStock.getProductId());
    }

    @Transactional
    public void releaseReservation(Long warehouseId, Long productId, Integer quantity) {
        log.info("Releasing reservation: warehouseId={}, productId={}, operationType=RELEASE",
                warehouseId, productId);
        validateWarehouseExists(warehouseId);

        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new StockNotAvailableException(
                        "Stock not found for product " + productId
                                + " in warehouse " + warehouseId));

        InventoryOperationService.ReservationMutation mutation =
                inventoryOperationService.releaseReservation(stock, quantity);
        StockLevel savedStock = inventoryOperationService.saveStockLevel(stock);

        stockMovementService.recordRelease(
                savedStock.getWarehouseId(),
                savedStock.getProductId(),
                mutation.quantityChanged(),
                mutation.previousReservedQuantity(),
                mutation.newReservedQuantity(),
                "Reservation released");
        syncStockSignals(savedStock);

        log.info("Completed reservation release: warehouseId={}, productId={}, operationType=RELEASE",
                savedStock.getWarehouseId(), savedStock.getProductId());
    }

    @Transactional
    public void transferStock(StockTransferDTO dto) {
        log.info("Transferring stock: productId={}, warehouseId={}, operationType=TRANSFER_OUT",
                dto.getProductId(), dto.getFromWarehouseId());

        if (dto.getFromWarehouseId().equals(dto.getToWarehouseId())) {
            throw new InvalidOperationException(
                    "Source and destination warehouses cannot be the same");
        }

        Warehouse sourceWarehouse = inventoryOperationService.getWarehouseForMutation(
                dto.getFromWarehouseId());
        Warehouse destinationWarehouse = inventoryOperationService.getWarehouseForMutation(
                dto.getToWarehouseId());

        StockLevel sourceStock = inventoryOperationService.getOrCreateStockLevel(
                dto.getFromWarehouseId(), dto.getProductId());
        StockLevel destinationStock = inventoryOperationService.getOrCreateStockLevel(
                dto.getToWarehouseId(), dto.getProductId());

        InventoryOperationService.ThresholdSettings thresholds =
                inventoryOperationService.resolveThresholds(
                        dto.getProductId(),
                        firstNonNull(sourceStock.getReorderLevel(), destinationStock.getReorderLevel()),
                        firstNonNull(sourceStock.getMaxStockLevel(), destinationStock.getMaxStockLevel()));
        inventoryOperationService.applyThresholds(sourceStock, thresholds);
        inventoryOperationService.applyThresholds(destinationStock, thresholds);

        // Existing transfer endpoint now uses issue + receipt mutations inside one transaction.
        InventoryOperationService.StockMutation sourceMutation =
                inventoryOperationService.handleIssue(
                        sourceWarehouse,
                        sourceStock,
                        dto.getQuantity(),
                        dto.getReason());
        InventoryOperationService.StockMutation destinationMutation =
                inventoryOperationService.handleReceipt(
                        destinationWarehouse,
                        destinationStock,
                        dto.getQuantity(),
                        dto.getReason());

        StockLevel savedSource = inventoryOperationService.saveStockLevel(sourceStock);
        StockLevel savedDestination = inventoryOperationService.saveStockLevel(destinationStock);
        inventoryOperationService.saveWarehouse(sourceWarehouse);
        inventoryOperationService.saveWarehouse(destinationWarehouse);

        stockMovementService.recordTransferOut(
                savedSource.getWarehouseId(),
                savedSource.getProductId(),
                dto.getQuantity(),
                sourceMutation.previousQuantity(),
                sourceMutation.newQuantity(),
                dto.getReason(),
                dto.getToWarehouseId());
        stockMovementService.recordTransferIn(
                savedDestination.getWarehouseId(),
                savedDestination.getProductId(),
                dto.getQuantity(),
                destinationMutation.previousQuantity(),
                destinationMutation.newQuantity(),
                dto.getReason(),
                dto.getFromWarehouseId());

        syncStockSignals(savedSource);
        syncStockSignals(savedDestination);

        log.info("Completed stock transfer: productId={}, warehouseId={}, operationType=TRANSFER_IN",
                savedDestination.getProductId(), savedDestination.getWarehouseId());
    }

    public List<StockLevelResponseDTO> getLowStockItems(Integer threshold) {
        log.info("Fetching low stock items using fallback threshold: {}", threshold);
        return stockLevelRepository.findLowStockItems(threshold)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private void recordStockUpdateMovement(
            StockLevel stock, InventoryOperationService.StockMutation mutation) {
        switch (mutation.operationType()) {
            case "RECEIPT" -> stockMovementService.recordReceipt(
                    stock.getWarehouseId(),
                    stock.getProductId(),
                    mutation.quantityChanged(),
                    mutation.previousQuantity(),
                    mutation.newQuantity(),
                    mutation.reason());
            case "ISSUE" -> stockMovementService.recordIssue(
                    stock.getWarehouseId(),
                    stock.getProductId(),
                    mutation.quantityChanged(),
                    mutation.previousQuantity(),
                    mutation.newQuantity(),
                    mutation.reason());
            default -> stockMovementService.recordAdjustment(
                    stock.getWarehouseId(),
                    stock.getProductId(),
                    mutation.quantityChanged(),
                    mutation.previousQuantity(),
                    mutation.newQuantity(),
                    mutation.reason());
        }
    }

    private void syncStockSignals(StockLevel stock) {
        stockAlertService.syncAlerts(stock, stock.getReorderLevel(), stock.getMaxStockLevel());
        checkAndPublishStockEvents(stock, stock.getReorderLevel(), stock.getMaxStockLevel());
    }

    private void checkAndPublishStockEvents(StockLevel stock,
            Integer reorderLevel, Integer maxStockLevel) {
        try {
            if (reorderLevel != null && stock.getAvailableQuantity() < reorderLevel) {
                log.warn("LOW STOCK detected! Product: {}, Warehouse: {}, Qty: {}",
                        stock.getProductId(), stock.getWarehouseId(),
                        stock.getAvailableQuantity());
                stockEventPublisher.publishLowStockEvent(
                        stock.getProductId(),
                        stock.getWarehouseId(),
                        stock.getAvailableQuantity(),
                        reorderLevel);
            }

            if (maxStockLevel != null && stock.getQuantity() > maxStockLevel) {
                log.warn("OVERSTOCK detected! Product: {}, Warehouse: {}, Qty: {}",
                        stock.getProductId(), stock.getWarehouseId(),
                        stock.getQuantity());
                stockEventPublisher.publishOverstockEvent(
                        stock.getProductId(),
                        stock.getWarehouseId(),
                        stock.getQuantity(),
                        maxStockLevel);
            }
        } catch (Exception ex) {
            log.error("Failed to publish stock event for product {} in warehouse {}: {}",
                    stock.getProductId(), stock.getWarehouseId(), ex.getMessage());
        }
    }

    private void validateWarehouseExists(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new WarehouseNotFoundException(
                    "Warehouse not found with ID: " + warehouseId);
        }
    }

    private Integer firstNonNull(Integer primary, Integer secondary) {
        return primary != null ? primary : secondary;
    }

    private StockLevelResponseDTO mapToDTO(StockLevel stock) {
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

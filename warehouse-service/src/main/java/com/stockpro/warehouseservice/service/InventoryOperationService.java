package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.client.ProductCatalogClient;
import com.stockpro.warehouseservice.dto.ProductLookupResponseDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.exception.CapacityExceededException;
import com.stockpro.warehouseservice.exception.InvalidOperationException;
import com.stockpro.warehouseservice.exception.ProductLookupException;
import com.stockpro.warehouseservice.exception.StockNotAvailableException;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryOperationService {

    private final StockLevelRepository stockLevelRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductCatalogClient productCatalogClient;

    public Warehouse getWarehouseForMutation(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + warehouseId));
        warehouse.setUsedCapacity(currentWarehouseUsage(warehouseId));
        return warehouse;
    }

    public StockLevel getOrCreateStockLevel(Long warehouseId, Long productId) {
        return stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseGet(() -> StockLevel.builder()
                        .warehouseId(warehouseId)
                        .productId(productId)
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());
    }

    public ThresholdSettings resolveThresholds(
            Long productId, Integer reorderLevel, Integer maxStockLevel) {
        Integer resolvedReorderLevel = reorderLevel;
        Integer resolvedMaxStockLevel = maxStockLevel;

        if (resolvedReorderLevel != null && resolvedMaxStockLevel != null) {
            return new ThresholdSettings(resolvedReorderLevel, resolvedMaxStockLevel);
        }

        try {
            ProductLookupResponseDTO product = productCatalogClient.getProductById(productId);
            if (resolvedReorderLevel == null) {
                resolvedReorderLevel = product.getReorderLevel();
            }
            if (resolvedMaxStockLevel == null) {
                resolvedMaxStockLevel = product.getMaxStockLevel();
            }
        } catch (ProductLookupException ex) {
            log.warn("Unable to resolve thresholds from product-service for product {}: {}",
                    productId, ex.getMessage());
        }

        return new ThresholdSettings(resolvedReorderLevel, resolvedMaxStockLevel);
    }

    public void applyThresholds(StockLevel stock, ThresholdSettings thresholds) {
        stock.setReorderLevel(thresholds.reorderLevel());
        stock.setMaxStockLevel(thresholds.maxStockLevel());
    }

    public StockMutation handleAdjustment(
            Warehouse warehouse, StockLevel stock, Integer targetQuantity, String reason) {
        if (targetQuantity == null || targetQuantity < 0) {
            throw new InvalidOperationException("Target quantity cannot be negative");
        }
        if (targetQuantity < defaultIfNull(stock.getReservedQuantity())) {
            throw new InvalidOperationException(
                    "Target quantity cannot be lower than reserved quantity");
        }

        int currentQuantity = defaultIfNull(stock.getQuantity());
        int delta = targetQuantity - currentQuantity;

        if (delta > 0) {
            return handleReceipt(warehouse, stock, delta, normalizeReason(
                    reason, "Stock receipt processed via stock update endpoint"));
        }
        if (delta < 0) {
            return handleIssue(warehouse, stock, Math.abs(delta), normalizeReason(
                    reason, "Stock issue processed via stock update endpoint"));
        }

        stock.setQuantity(targetQuantity);
        validateStockState(stock);
        return new StockMutation("ADJUSTMENT", 0, currentQuantity, targetQuantity,
                normalizeReason(reason, "Stock adjusted with no quantity delta"));
    }

    public StockMutation handleReceipt(
            Warehouse warehouse, StockLevel stock, Integer receiptQuantity, String reason) {
        validatePositiveQuantity(receiptQuantity, "Receipt quantity must be at least 1");

        int previousQuantity = defaultIfNull(stock.getQuantity());
        int updatedQuantity = previousQuantity + receiptQuantity;
        ensureWarehouseCapacity(warehouse, receiptQuantity);

        stock.setQuantity(updatedQuantity);
        warehouse.setUsedCapacity(defaultIfNull(warehouse.getUsedCapacity()) + receiptQuantity);
        validateStockState(stock);

        return new StockMutation(
                "RECEIPT",
                receiptQuantity,
                previousQuantity,
                updatedQuantity,
                reason);
    }

    public StockMutation handleIssue(
            Warehouse warehouse, StockLevel stock, Integer issueQuantity, String reason) {
        validatePositiveQuantity(issueQuantity, "Issue quantity must be at least 1");

        int previousQuantity = defaultIfNull(stock.getQuantity());
        if (stock.getAvailableQuantity() < issueQuantity) {
            throw new StockNotAvailableException(
                    "Insufficient available stock. Available: "
                            + stock.getAvailableQuantity()
                            + ", Requested: " + issueQuantity);
        }

        int updatedQuantity = previousQuantity - issueQuantity;
        if (updatedQuantity < 0) {
            throw new InvalidOperationException("Stock quantity cannot be negative");
        }

        stock.setQuantity(updatedQuantity);
        warehouse.setUsedCapacity(defaultIfNull(warehouse.getUsedCapacity()) - issueQuantity);
        validateStockState(stock);
        validateWarehouseState(warehouse);

        return new StockMutation(
                "ISSUE",
                -issueQuantity,
                previousQuantity,
                updatedQuantity,
                reason);
    }

    public ReservationMutation reserveStock(StockLevel stock, Integer quantity) {
        validatePositiveQuantity(quantity, "Reservation quantity must be at least 1");
        if (stock.getAvailableQuantity() < quantity) {
            throw new StockNotAvailableException(
                    "Insufficient available stock. Available: "
                            + stock.getAvailableQuantity()
                            + ", Requested: " + quantity);
        }

        int previousReserved = defaultIfNull(stock.getReservedQuantity());
        stock.setReservedQuantity(previousReserved + quantity);
        validateStockState(stock);

        return new ReservationMutation(quantity, previousReserved, stock.getReservedQuantity());
    }

    public ReservationMutation releaseReservation(StockLevel stock, Integer quantity) {
        validatePositiveQuantity(quantity, "Release quantity must be at least 1");

        int previousReserved = defaultIfNull(stock.getReservedQuantity());
        if (previousReserved < quantity) {
            throw new InvalidOperationException(
                    "Cannot release more stock than is currently reserved");
        }

        stock.setReservedQuantity(previousReserved - quantity);
        validateStockState(stock);

        return new ReservationMutation(quantity, previousReserved, stock.getReservedQuantity());
    }

    public void applyBinLocation(StockLevel stock, String binLocation) {
        if (binLocation != null && !binLocation.isBlank()) {
            stock.setBinLocation(binLocation);
        }
    }

    public StockLevel saveStockLevel(StockLevel stock) {
        validateStockState(stock);
        return stockLevelRepository.save(stock);
    }

    public Warehouse saveWarehouse(Warehouse warehouse) {
        validateWarehouseState(warehouse);
        return warehouseRepository.save(warehouse);
    }

    private void ensureWarehouseCapacity(Warehouse warehouse, Integer quantityToAdd) {
        int projectedUsedCapacity = defaultIfNull(warehouse.getUsedCapacity()) + quantityToAdd;
        if (projectedUsedCapacity > defaultIfNull(warehouse.getCapacity())) {
            throw new CapacityExceededException(
                    "Warehouse capacity exceeded for warehouse "
                            + warehouse.getWarehouseId()
                            + ". Capacity: " + warehouse.getCapacity()
                            + ", Requested usage: " + projectedUsedCapacity);
        }
    }

    private void validateWarehouseState(Warehouse warehouse) {
        int usedCapacity = defaultIfNull(warehouse.getUsedCapacity());
        int capacity = defaultIfNull(warehouse.getCapacity());

        if (usedCapacity < 0) {
            throw new InvalidOperationException("Warehouse used capacity cannot be negative");
        }
        if (usedCapacity > capacity) {
            throw new CapacityExceededException(
                    "Warehouse capacity exceeded for warehouse " + warehouse.getWarehouseId());
        }
    }

    private void validateStockState(StockLevel stock) {
        int quantity = defaultIfNull(stock.getQuantity());
        int reservedQuantity = defaultIfNull(stock.getReservedQuantity());

        if (quantity < 0) {
            throw new InvalidOperationException("Stock quantity cannot be negative");
        }
        if (reservedQuantity < 0) {
            throw new InvalidOperationException("Reserved quantity cannot be negative");
        }
        if (reservedQuantity > quantity) {
            throw new InvalidOperationException(
                    "Reserved quantity cannot exceed total quantity");
        }
    }

    private int currentWarehouseUsage(Long warehouseId) {
        return defaultIfNull(stockLevelRepository.sumQuantityByWarehouseId(warehouseId));
    }

    private void validatePositiveQuantity(Integer quantity, String message) {
        if (quantity == null || quantity < 1) {
            throw new InvalidOperationException(message);
        }
    }

    private String normalizeReason(String reason, String fallbackReason) {
        if (reason == null || reason.isBlank()) {
            return fallbackReason;
        }
        return reason;
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    public record ThresholdSettings(Integer reorderLevel, Integer maxStockLevel) {
    }

    public record StockMutation(
            String operationType,
            Integer quantityChanged,
            Integer previousQuantity,
            Integer newQuantity,
            String reason) {
    }

    public record ReservationMutation(
            Integer quantityChanged,
            Integer previousReservedQuantity,
            Integer newReservedQuantity) {
    }
}

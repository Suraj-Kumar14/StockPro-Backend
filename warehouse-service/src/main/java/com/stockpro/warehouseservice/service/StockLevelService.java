package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.dto.*;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.exception.*;
import com.stockpro.warehouseservice.rabbitmq.StockEventPublisher;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class StockLevelService {

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockEventPublisher stockEventPublisher;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private StockAlertService stockAlertService;

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
                .stream().map(this::mapToDTO).toList();
    }

    public List<StockLevelResponseDTO> getStockByProduct(Long productId) {
        log.info("Fetching stock across all warehouses for product: {}", productId);
        return stockLevelRepository.findByProductId(productId)
                .stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public StockLevelResponseDTO updateStock(Long warehouseId, StockUpdateDTO dto) {
        log.info("Updating stock for product {} in warehouse {}",
                dto.getProductId(), warehouseId);
        validateWarehouseExists(warehouseId);

        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, dto.getProductId())
                .orElseGet(() -> StockLevel.builder()
                        .warehouseId(warehouseId)
                        .productId(dto.getProductId())
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

        int previousQuantity = stock.getQuantity();
        stock.setQuantity(dto.getQuantity());
        if (dto.getBinLocation() != null) {
            stock.setBinLocation(dto.getBinLocation());
        }

        StockLevel saved = stockLevelRepository.save(stock);
        stockMovementService.recordAdjustment(
                saved.getWarehouseId(),
                saved.getProductId(),
                dto.getQuantity() - previousQuantity,
                previousQuantity,
                saved.getQuantity(),
                "Stock updated via stock update endpoint");
        stockAlertService.syncAlerts(saved, dto.getReorderLevel(), dto.getMaxStockLevel());
        checkAndPublishStockEvents(saved, dto.getReorderLevel(), dto.getMaxStockLevel());

        log.info("Stock updated successfully");
        return mapToDTO(saved);
    }

    @Transactional
    public void reserveStock(Long warehouseId, Long productId, Integer quantity) {
        log.info("Reserving {} units of product {} in warehouse {}",
                quantity, productId, warehouseId);

        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Stock not found for product " + productId));

        if (stock.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available: "
                            + stock.getAvailableQuantity()
                            + ", Requested: " + quantity);
        }

        int previousReservedQuantity = stock.getReservedQuantity();
        stock.setReservedQuantity(stock.getReservedQuantity() + quantity);
        StockLevel saved = stockLevelRepository.save(stock);
        stockMovementService.recordReservation(
                saved.getWarehouseId(),
                saved.getProductId(),
                quantity,
                previousReservedQuantity,
                saved.getReservedQuantity(),
                "Stock reserved");
        log.info("Stock reserved successfully");
    }

    @Transactional
    public void releaseReservation(Long warehouseId, Long productId, Integer quantity) {
        log.info("Releasing reservation of {} units of product {} in warehouse {}",
                quantity, productId, warehouseId);

        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Stock not found for product " + productId));

        int previousReservedQuantity = stock.getReservedQuantity();
        int newReserved = stock.getReservedQuantity() - quantity;
        stock.setReservedQuantity(Math.max(0, newReserved));
        StockLevel saved = stockLevelRepository.save(stock);
        stockMovementService.recordRelease(
                saved.getWarehouseId(),
                saved.getProductId(),
                previousReservedQuantity - saved.getReservedQuantity(),
                previousReservedQuantity,
                saved.getReservedQuantity(),
                "Reservation released");
    }

    @Transactional
    public void transferStock(StockTransferDTO dto) {
        log.info("Transferring {} units of product {} from warehouse {} to {}",
                dto.getQuantity(), dto.getProductId(),
                dto.getFromWarehouseId(), dto.getToWarehouseId());

        if (dto.getFromWarehouseId().equals(dto.getToWarehouseId())) {
            throw new IllegalArgumentException(
                    "Source and destination warehouses cannot be the same");
        }

        validateWarehouseExists(dto.getFromWarehouseId());
        validateWarehouseExists(dto.getToWarehouseId());

        StockLevel source = stockLevelRepository
                .findByWarehouseIdAndProductId(
                        dto.getFromWarehouseId(), dto.getProductId())
                .orElseThrow(() -> new InsufficientStockException(
                        "No stock found in source warehouse for product "
                                + dto.getProductId()));

        if (source.getAvailableQuantity() < dto.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock in source warehouse. Available: "
                            + source.getAvailableQuantity()
                            + ", Requested: " + dto.getQuantity());
        }

        int sourcePreviousQuantity = source.getQuantity();
        source.setQuantity(source.getQuantity() - dto.getQuantity());
        StockLevel savedSource = stockLevelRepository.save(source);
        stockMovementService.recordTransferOut(
                savedSource.getWarehouseId(),
                savedSource.getProductId(),
                dto.getQuantity(),
                sourcePreviousQuantity,
                savedSource.getQuantity(),
                dto.getReason(),
                dto.getToWarehouseId());

        StockLevel destination = stockLevelRepository
                .findByWarehouseIdAndProductId(
                        dto.getToWarehouseId(), dto.getProductId())
                .orElseGet(() -> StockLevel.builder()
                        .warehouseId(dto.getToWarehouseId())
                        .productId(dto.getProductId())
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

        int destinationPreviousQuantity = destination.getQuantity();
        destination.setQuantity(destination.getQuantity() + dto.getQuantity());
        StockLevel savedDestination = stockLevelRepository.save(destination);
        stockMovementService.recordTransferIn(
                savedDestination.getWarehouseId(),
                savedDestination.getProductId(),
                dto.getQuantity(),
                destinationPreviousQuantity,
                savedDestination.getQuantity(),
                dto.getReason(),
                dto.getFromWarehouseId());

        log.info("Stock transfer completed successfully");
    }

    public List<StockLevelResponseDTO> getLowStockItems(Integer threshold) {
        log.info("Fetching low stock items below threshold: {}", threshold);
        return stockLevelRepository.findLowStockItems(threshold)
                .stream().map(this::mapToDTO).toList();
    }

    private void checkAndPublishStockEvents(StockLevel stock,
            Integer reorderLevel, Integer maxStockLevel) {
        try {
            if (reorderLevel != null && stock.getQuantity() <= reorderLevel) {
                log.warn("LOW STOCK detected! Product: {}, Warehouse: {}, Qty: {}",
                        stock.getProductId(), stock.getWarehouseId(),
                        stock.getQuantity());
                stockEventPublisher.publishLowStockEvent(
                        stock.getProductId(),
                        stock.getWarehouseId(),
                        stock.getQuantity(),
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
        } catch (Exception e) {
            log.error("Failed to publish stock event: {}", e.getMessage());
        }
    }

    private void validateWarehouseExists(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new WarehouseNotFoundException(
                    "Warehouse not found with ID: " + warehouseId);
        }
    }

    private StockLevelResponseDTO mapToDTO(StockLevel s) {
        return StockLevelResponseDTO.builder()
                .stockId(s.getStockId())
                .warehouseId(s.getWarehouseId())
                .productId(s.getProductId())
                .quantity(s.getQuantity())
                .reservedQuantity(s.getReservedQuantity())
                .availableQuantity(s.getAvailableQuantity())
                .binLocation(s.getBinLocation())
                .lastUpdated(s.getLastUpdated())
                .build();
    }
}

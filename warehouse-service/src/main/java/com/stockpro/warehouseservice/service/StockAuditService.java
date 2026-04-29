package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.dto.StockAuditRequestDTO;
import com.stockpro.warehouseservice.dto.StockAuditResponseDTO;
import com.stockpro.warehouseservice.dto.StockLevelResponseDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class StockAuditService {

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private StockAlertService stockAlertService;

    @Transactional
    public StockAuditResponseDTO performAudit(StockAuditRequestDTO dto) {
        validateWarehouseExists(dto.getWarehouseId());

        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(dto.getWarehouseId(), dto.getProductId())
                .orElseGet(() -> StockLevel.builder()
                        .warehouseId(dto.getWarehouseId())
                        .productId(dto.getProductId())
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

        int systemQuantity = stock.getQuantity();
        int discrepancy = dto.getCountedQuantity() - systemQuantity;

        stock.setQuantity(dto.getCountedQuantity());
        if (dto.getBinLocation() != null) {
            stock.setBinLocation(dto.getBinLocation());
        }

        StockLevel saved = stockLevelRepository.save(stock);
        stockMovementService.recordAudit(
                saved.getWarehouseId(),
                saved.getProductId(),
                discrepancy,
                systemQuantity,
                saved.getQuantity(),
                dto.getReason());
        stockAlertService.syncAlerts(saved, dto.getReorderLevel(), dto.getMaxStockLevel());

        log.info("Completed stock audit for warehouse {} and product {}",
                dto.getWarehouseId(), dto.getProductId());

        return StockAuditResponseDTO.builder()
                .warehouseId(saved.getWarehouseId())
                .productId(saved.getProductId())
                .systemQuantity(systemQuantity)
                .countedQuantity(saved.getQuantity())
                .discrepancy(discrepancy)
                .reason(dto.getReason())
                .updatedStock(mapToDto(saved))
                .build();
    }

    private void validateWarehouseExists(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new WarehouseNotFoundException(
                    "Warehouse not found with ID: " + warehouseId);
        }
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

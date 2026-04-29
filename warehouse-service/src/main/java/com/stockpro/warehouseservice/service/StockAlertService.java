package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.dto.StockAlertResponseDTO;
import com.stockpro.warehouseservice.entity.StockAlert;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.StockAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class StockAlertService {

    @Autowired
    private StockAlertRepository stockAlertRepository;

    public void syncAlerts(StockLevel stock, Integer reorderLevel,
            Integer maxStockLevel) {
        if (reorderLevel != null) {
            syncAlert(stock, "LOW_STOCK", reorderLevel,
                    stock.getQuantity() <= reorderLevel,
                    "Low stock threshold reached");
        }

        if (maxStockLevel != null) {
            syncAlert(stock, "OVERSTOCK", maxStockLevel,
                    stock.getQuantity() > maxStockLevel,
                    "Maximum stock threshold exceeded");
        }
    }

    public List<StockAlertResponseDTO> getActiveAlerts() {
        return stockAlertRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public StockAlertResponseDTO acknowledgeAlert(Long alertId, String acknowledgedBy) {
        StockAlert alert = stockAlertRepository.findByAlertIdAndActiveTrue(alertId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Active alert not found with ID: " + alertId));
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(LocalDateTime.now());
        StockAlert saved = stockAlertRepository.save(alert);
        log.info("Acknowledged alert {} by {}", alertId, acknowledgedBy);
        return mapToDto(saved);
    }

    private void syncAlert(StockLevel stock, String alertType, Integer threshold,
            boolean shouldBeActive, String messagePrefix) {
        StockAlert alert = stockAlertRepository
                .findByWarehouseIdAndProductIdAndAlertTypeAndActiveTrue(
                        stock.getWarehouseId(), stock.getProductId(), alertType)
                .orElseGet(() -> StockAlert.builder()
                        .warehouseId(stock.getWarehouseId())
                        .productId(stock.getProductId())
                        .alertType(alertType)
                        .build());

        if (shouldBeActive) {
            alert.setActive(true);
            alert.setAcknowledged(false);
            alert.setAcknowledgedAt(null);
            alert.setAcknowledgedBy(null);
            alert.setCurrentQuantity(stock.getQuantity());
            alert.setThresholdValue(threshold);
            alert.setMessage(messagePrefix + " for product "
                    + stock.getProductId() + " in warehouse "
                    + stock.getWarehouseId());
            stockAlertRepository.save(alert);
        } else if (alert.getAlertId() != null) {
            alert.setActive(false);
            alert.setCurrentQuantity(stock.getQuantity());
            alert.setThresholdValue(threshold);
            stockAlertRepository.save(alert);
        }
    }

    private StockAlertResponseDTO mapToDto(StockAlert alert) {
        return StockAlertResponseDTO.builder()
                .alertId(alert.getAlertId())
                .warehouseId(alert.getWarehouseId())
                .productId(alert.getProductId())
                .alertType(alert.getAlertType())
                .currentQuantity(alert.getCurrentQuantity())
                .thresholdValue(alert.getThresholdValue())
                .active(alert.getActive())
                .acknowledged(alert.getAcknowledged())
                .acknowledgedBy(alert.getAcknowledgedBy())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .message(alert.getMessage())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}

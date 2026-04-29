package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.dto.StockAlertResponseDTO;
import com.stockpro.warehouseservice.entity.StockAlert;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.repository.StockAlertRepository;
import com.stockpro.warehouseservice.service.StockAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceTest {

    @Mock
    private StockAlertRepository stockAlertRepository;

    @InjectMocks
    private StockAlertService stockAlertService;

    @Test
    void syncAlerts_shouldCreateLowStockAlert_whenQuantityFallsBelowThreshold() {
        StockLevel stock = StockLevel.builder()
                .warehouseId(1L)
                .productId(101L)
                .quantity(5)
                .reservedQuantity(0)
                .build();

        when(stockAlertRepository.findByWarehouseIdAndProductIdAndAlertTypeAndActiveTrue(
                1L, 101L, "LOW_STOCK")).thenReturn(Optional.empty());

        stockAlertService.syncAlerts(stock, 10, null);

        verify(stockAlertRepository).save(any(StockAlert.class));
    }

    @Test
    void acknowledgeAlert_shouldMarkAlertAcknowledged_whenAlertExists() {
        StockAlert alert = StockAlert.builder()
                .alertId(9L)
                .warehouseId(1L)
                .productId(101L)
                .alertType("LOW_STOCK")
                .currentQuantity(4)
                .thresholdValue(10)
                .active(true)
                .acknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(stockAlertRepository.findByAlertIdAndActiveTrue(9L))
                .thenReturn(Optional.of(alert));
        when(stockAlertRepository.save(any(StockAlert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockAlertResponseDTO result = stockAlertService.acknowledgeAlert(9L, "warehouse.user");

        assertTrue(result.getAcknowledged());
        assertEquals("warehouse.user", result.getAcknowledgedBy());
    }

    @Test
    void getActiveAlerts_shouldReturnOnlyActiveAlerts_whenRepositoryHasAlerts() {
        StockAlert alert = StockAlert.builder()
                .alertId(9L)
                .warehouseId(1L)
                .productId(101L)
                .alertType("LOW_STOCK")
                .currentQuantity(4)
                .thresholdValue(10)
                .active(true)
                .acknowledged(false)
                .message("Low stock threshold reached")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(stockAlertRepository.findByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(alert));

        List<StockAlertResponseDTO> result = stockAlertService.getActiveAlerts();

        assertEquals(1, result.size());
        assertFalse(result.get(0).getAcknowledged());
    }
}

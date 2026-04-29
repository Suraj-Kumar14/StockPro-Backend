package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.dto.StockAuditRequestDTO;
import com.stockpro.warehouseservice.dto.StockAuditResponseDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import com.stockpro.warehouseservice.service.StockAlertService;
import com.stockpro.warehouseservice.service.StockAuditService;
import com.stockpro.warehouseservice.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAuditServiceTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private StockAlertService stockAlertService;

    @InjectMocks
    private StockAuditService stockAuditService;

    private StockAuditRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new StockAuditRequestDTO();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setCountedQuantity(42);
        request.setReason("Cycle count discrepancy");
        request.setBinLocation("A-10");
        request.setReorderLevel(10);
        request.setMaxStockLevel(100);
    }

    @Test
    void performAudit_shouldUpdateStockAndRecordDiscrepancy_whenWarehouseExists() {
        StockLevel existing = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(100L)
                .quantity(50)
                .reservedQuantity(5)
                .binLocation("A-01")
                .build();

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L))
                .thenReturn(Optional.of(existing));
        when(stockLevelRepository.save(any(StockLevel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockAuditResponseDTO result = stockAuditService.performAudit(request);

        assertEquals(50, result.getSystemQuantity());
        assertEquals(42, result.getCountedQuantity());
        assertEquals(-8, result.getDiscrepancy());
        verify(stockMovementService)
                .recordAudit(1L, 100L, -8, 50, 42, "Cycle count discrepancy");
        verify(stockAlertService).syncAlerts(any(StockLevel.class), any(), any());
    }

    @Test
    void performAudit_shouldThrowException_whenWarehouseDoesNotExist() {
        when(warehouseRepository.existsById(1L)).thenReturn(false);

        WarehouseNotFoundException exception = assertThrows(
                WarehouseNotFoundException.class,
                () -> stockAuditService.performAudit(request)
        );

        assertEquals("Warehouse not found with ID: 1", exception.getMessage());
    }
}

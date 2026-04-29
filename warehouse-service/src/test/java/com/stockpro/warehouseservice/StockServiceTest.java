package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.dto.StockLevelResponseDTO;
import com.stockpro.warehouseservice.dto.StockUpdateDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.rabbitmq.StockEventPublisher;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import com.stockpro.warehouseservice.service.StockAlertService;
import com.stockpro.warehouseservice.service.StockLevelService;
import com.stockpro.warehouseservice.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockEventPublisher stockEventPublisher;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private StockAlertService stockAlertService;

    @InjectMocks
    private StockLevelService stockLevelService;

    private StockLevel stockLevel;

    @BeforeEach
    void setUp() {
        stockLevel = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(1001L)
                .quantity(80)
                .reservedQuantity(10)
                .binLocation("R1-S1")
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Test
    void getStockByWarehouse_shouldReturnStockLevels_whenWarehouseExists() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseId(1L)).thenReturn(List.of(stockLevel));

        List<StockLevelResponseDTO> result = stockLevelService.getStockByWarehouse(1L);

        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).getProductId());
        assertEquals(70, result.get(0).getAvailableQuantity());
    }

    @Test
    void getStockByWarehouse_shouldThrowException_whenWarehouseDoesNotExist() {
        when(warehouseRepository.existsById(99L)).thenReturn(false);

        WarehouseNotFoundException exception = assertThrows(
                WarehouseNotFoundException.class,
                () -> stockLevelService.getStockByWarehouse(99L)
        );

        assertEquals("Warehouse not found with ID: 99", exception.getMessage());
        verify(stockLevelRepository, never()).findByWarehouseId(99L);
    }

    @Test
    void getStockByProduct_shouldReturnStockAcrossWarehouses_whenProductIsTracked() {
        StockLevel secondWarehouseStock = StockLevel.builder()
                .stockId(2L)
                .warehouseId(2L)
                .productId(1001L)
                .quantity(30)
                .reservedQuantity(5)
                .binLocation("R2-S4")
                .build();

        when(stockLevelRepository.findByProductId(1001L)).thenReturn(List.of(stockLevel, secondWarehouseStock));

        List<StockLevelResponseDTO> result = stockLevelService.getStockByProduct(1001L);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getWarehouseId());
        assertEquals(2L, result.get(1).getWarehouseId());
    }

    @Test
    void updateStock_shouldCreateNewStockRecord_whenStockDoesNotExist() {
        StockUpdateDTO request = new StockUpdateDTO();
        request.setProductId(2002L);
        request.setQuantity(45);
        request.setBinLocation("NEW-BIN");

        StockLevel createdStock = StockLevel.builder()
                .stockId(3L)
                .warehouseId(1L)
                .productId(2002L)
                .quantity(45)
                .reservedQuantity(0)
                .binLocation("NEW-BIN")
                .build();

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 2002L)).thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(createdStock);

        StockLevelResponseDTO result = stockLevelService.updateStock(1L, request);

        assertEquals(45, result.getQuantity());
        assertEquals("NEW-BIN", result.getBinLocation());
        verify(stockLevelRepository).save(any(StockLevel.class));
    }

    @Test
    void updateStock_shouldUpdateExistingStockAndPublishLowStockEvent_whenQuantityReachesReorderLevel() {
        StockUpdateDTO request = new StockUpdateDTO();
        request.setProductId(1001L);
        request.setQuantity(10);
        request.setBinLocation("LOW-STOCK-BIN");
        request.setReorderLevel(10);

        StockLevel savedStock = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(1001L)
                .quantity(10)
                .reservedQuantity(0)
                .binLocation("LOW-STOCK-BIN")
                .build();

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1001L)).thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(savedStock);

        StockLevelResponseDTO result = stockLevelService.updateStock(1L, request);

        assertEquals(10, result.getQuantity());
        assertEquals("LOW-STOCK-BIN", result.getBinLocation());
        verify(stockEventPublisher).publishLowStockEvent(1001L, 1L, 10, 10);
    }

    @Test
    void updateStock_shouldPublishOverstockEvent_whenQuantityExceedsMaximumLevel() {
        StockUpdateDTO request = new StockUpdateDTO();
        request.setProductId(1001L);
        request.setQuantity(250);
        request.setMaxStockLevel(200);

        StockLevel savedStock = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(1001L)
                .quantity(250)
                .reservedQuantity(0)
                .binLocation("R1-S1")
                .build();

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1001L)).thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(savedStock);

        stockLevelService.updateStock(1L, request);

        verify(stockEventPublisher).publishOverstockEvent(1001L, 1L, 250, 200);
    }

    @Test
    void updateStock_shouldNotFail_whenEventPublishingThrowsException() {
        StockUpdateDTO request = new StockUpdateDTO();
        request.setProductId(1001L);
        request.setQuantity(5);
        request.setReorderLevel(10);

        StockLevel savedStock = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(1001L)
                .quantity(5)
                .reservedQuantity(0)
                .build();

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1001L)).thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(savedStock);
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(stockEventPublisher)
                .publishLowStockEvent(1001L, 1L, 5, 10);

        assertDoesNotThrow(() -> stockLevelService.updateStock(1L, request));
    }

    @Test
    void getLowStockItems_shouldReturnItemsBelowThreshold_whenRepositoryFindsMatches() {
        when(stockLevelRepository.findLowStockItems(15)).thenReturn(List.of(stockLevel));

        List<StockLevelResponseDTO> result = stockLevelService.getLowStockItems(15);

        assertEquals(1, result.size());
        assertEquals(1001L, result.get(0).getProductId());
    }
}

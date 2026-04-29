package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.dto.StockTransferDTO;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.exception.InsufficientStockException;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

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

    private StockLevel sourceStock;

    @BeforeEach
    void setUp() {
        sourceStock = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(501L)
                .quantity(100)
                .reservedQuantity(20)
                .binLocation("SRC-A1")
                .build();
    }

    @Test
    void reserveStock_shouldIncreaseReservedQuantity_whenAvailableStockExists() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L)).thenReturn(Optional.of(sourceStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        stockLevelService.reserveStock(1L, 501L, 30);

        assertEquals(50, sourceStock.getReservedQuantity());
        verify(stockLevelRepository).save(sourceStock);
    }

    @Test
    void reserveStock_shouldThrowException_whenAvailableStockIsInsufficient() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L)).thenReturn(Optional.of(sourceStock));

        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> stockLevelService.reserveStock(1L, 501L, 81)
        );

        assertEquals("Insufficient stock. Available: 80, Requested: 81", exception.getMessage());
    }

    @Test
    void releaseReservation_shouldNotDropBelowZero_whenReleaseExceedsReservedQuantity() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L)).thenReturn(Optional.of(sourceStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        stockLevelService.releaseReservation(1L, 501L, 50);

        assertEquals(0, sourceStock.getReservedQuantity());
        verify(stockLevelRepository).save(sourceStock);
    }

    @Test
    void transferStock_shouldMoveQuantityBetweenWarehouses_whenRequestIsValid() {
        StockTransferDTO request = new StockTransferDTO();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(2L);
        request.setProductId(501L);
        request.setQuantity(40);
        request.setReason("Transfer for customer demand");

        StockLevel destinationStock = StockLevel.builder()
                .stockId(2L)
                .warehouseId(2L)
                .productId(501L)
                .quantity(15)
                .reservedQuantity(0)
                .binLocation("DEST-B2")
                .build();

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseRepository.existsById(2L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L)).thenReturn(Optional.of(sourceStock));
        when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 501L)).thenReturn(Optional.of(destinationStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        stockLevelService.transferStock(request);

        assertEquals(60, sourceStock.getQuantity());
        assertEquals(55, destinationStock.getQuantity());
        verify(stockLevelRepository, times(2)).save(any(StockLevel.class));
    }

    @Test
    void transferStock_shouldCreateDestinationStock_whenDestinationEntryIsMissing() {
        StockTransferDTO request = new StockTransferDTO();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(3L);
        request.setProductId(501L);
        request.setQuantity(25);
        request.setReason("New warehouse stocking");

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseRepository.existsById(3L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L)).thenReturn(Optional.of(sourceStock));
        when(stockLevelRepository.findByWarehouseIdAndProductId(3L, 501L)).thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        stockLevelService.transferStock(request);

        assertEquals(75, sourceStock.getQuantity());
        verify(stockLevelRepository, times(2)).save(any(StockLevel.class));
    }

    @Test
    void transferStock_shouldThrowException_whenSourceAndDestinationWarehousesMatch() {
        StockTransferDTO request = new StockTransferDTO();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(1L);
        request.setProductId(501L);
        request.setQuantity(5);
        request.setReason("Invalid transfer");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> stockLevelService.transferStock(request)
        );

        assertEquals("Source and destination warehouses cannot be the same", exception.getMessage());
    }

    @Test
    void transferStock_shouldThrowException_whenSourceWarehouseHasNoStock() {
        StockTransferDTO request = new StockTransferDTO();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(2L);
        request.setProductId(501L);
        request.setQuantity(10);
        request.setReason("Urgent transfer");

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseRepository.existsById(2L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L)).thenReturn(Optional.empty());

        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> stockLevelService.transferStock(request)
        );

        assertEquals("No stock found in source warehouse for product 501", exception.getMessage());
    }

    @Test
    void transferStock_shouldThrowException_whenRequestedQuantityExceedsAvailableStock() {
        StockTransferDTO request = new StockTransferDTO();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(2L);
        request.setProductId(501L);
        request.setQuantity(81);
        request.setReason("Over-allocation test");

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseRepository.existsById(2L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L)).thenReturn(Optional.of(sourceStock));

        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> stockLevelService.transferStock(request)
        );

        assertEquals("Insufficient stock in source warehouse. Available: 80, Requested: 81", exception.getMessage());
    }
}

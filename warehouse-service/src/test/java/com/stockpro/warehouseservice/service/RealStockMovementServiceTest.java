package com.stockpro.warehouseservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.warehouseservice.dto.StockMovementResponseDTO;
import com.stockpro.warehouseservice.entity.StockMovement;
import com.stockpro.warehouseservice.repository.StockMovementRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RealStockMovementServiceTest {

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private StockMovementService stockMovementService;

    @Test
    void recordMethods_shouldPersistExpectedMovementTypes() {
        stockMovementService.recordReceipt(1L, 10L, 5, 10, 15, "receipt");
        stockMovementService.recordIssue(1L, 10L, -2, 15, 13, "issue");
        stockMovementService.recordAdjustment(1L, 10L, 3, 13, 16, "adjustment");
        stockMovementService.recordReservation(1L, 10L, 4, 1, 5, "reserve");
        stockMovementService.recordRelease(1L, 10L, 2, 5, 3, "release");
        stockMovementService.recordTransferOut(1L, 10L, -6, 16, 10, "transfer out", 2L);
        stockMovementService.recordTransferIn(2L, 10L, 6, 4, 10, "transfer in", 1L);
        stockMovementService.recordAudit(1L, 10L, -1, 10, 9, "audit");

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, times(8)).save(captor.capture());

        List<StockMovement> saved = captor.getAllValues();
        assertEquals("RECEIPT", saved.get(0).getMovementType());
        assertEquals("ISSUE", saved.get(1).getMovementType());
        assertEquals("ADJUSTMENT", saved.get(2).getMovementType());
        assertEquals("RESERVATION", saved.get(3).getMovementType());
        assertEquals("RELEASE", saved.get(4).getMovementType());
        assertEquals("TRANSFER_OUT", saved.get(5).getMovementType());
        assertEquals(2L, saved.get(5).getRelatedWarehouseId());
        assertEquals("TRANSFER_IN", saved.get(6).getMovementType());
        assertEquals(1L, saved.get(6).getRelatedWarehouseId());
        assertEquals("AUDIT", saved.get(7).getMovementType());
    }

    @Test
    void getMovementHistory_shouldUseWarehouseAndProductSpecificQuery() {
        StockMovement movement = movement(1L, 10L, "ISSUE", LocalDateTime.now());
        when(stockMovementRepository.findByWarehouseIdAndProductIdOrderByCreatedAtDesc(1L, 10L))
                .thenReturn(List.of(movement));

        List<StockMovementResponseDTO> result = stockMovementService.getMovementHistory(1L, 10L);

        assertEquals(1, result.size());
        assertEquals("ISSUE", result.get(0).getMovementType());
        verify(stockMovementRepository).findByWarehouseIdAndProductIdOrderByCreatedAtDesc(1L, 10L);
    }

    @Test
    void getMovementHistory_shouldUseWarehouseOnlyAndProductOnlyQueries() {
        when(stockMovementRepository.findByWarehouseIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(movement(1L, 10L, "RECEIPT", LocalDateTime.now())));
        when(stockMovementRepository.findByProductIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(movement(2L, 10L, "TRANSFER_IN", LocalDateTime.now())));

        List<StockMovementResponseDTO> byWarehouse = stockMovementService.getMovementHistory(1L, null);
        List<StockMovementResponseDTO> byProduct = stockMovementService.getMovementHistory(null, 10L);

        assertEquals("RECEIPT", byWarehouse.get(0).getMovementType());
        assertEquals("TRANSFER_IN", byProduct.get(0).getMovementType());
    }

    @Test
    void getMovementHistory_shouldSortAllRecordsWhenNoFilterProvided() {
        StockMovement older = movement(1L, 10L, "ISSUE", LocalDateTime.now().minusHours(2));
        StockMovement newer = movement(1L, 10L, "RECEIPT", LocalDateTime.now());
        when(stockMovementRepository.findAll()).thenReturn(List.of(older, newer));

        List<StockMovementResponseDTO> result = stockMovementService.getMovementHistory(null, null);

        assertEquals(2, result.size());
        assertEquals("RECEIPT", result.get(0).getMovementType());
        assertEquals("ISSUE", result.get(1).getMovementType());
    }

    private StockMovement movement(
            Long warehouseId,
            Long productId,
            String movementType,
            LocalDateTime createdAt) {
        return StockMovement.builder()
                .movementId(1L)
                .warehouseId(warehouseId)
                .productId(productId)
                .movementType(movementType)
                .quantityChanged(5)
                .previousQuantity(10)
                .newQuantity(15)
                .reason("test")
                .createdAt(createdAt)
                .build();
    }
}

package com.stockpro.movementservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.movementservice.dto.request.CreateMovementFromEventRequest;
import com.stockpro.movementservice.dto.request.CreateMovementRequest;
import com.stockpro.movementservice.dto.request.ReverseMovementRequest;
import com.stockpro.movementservice.dto.response.MovementSummaryResponse;
import com.stockpro.movementservice.entity.StockMovement;
import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementReasonCode;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import com.stockpro.movementservice.exception.InvalidMovementException;
import com.stockpro.movementservice.exception.MovementNotFoundException;
import com.stockpro.movementservice.repository.StockMovementRepository;
import com.stockpro.movementservice.service.MovementMapper;
import com.stockpro.movementservice.service.MovementEventPublisher;
import com.stockpro.movementservice.service.impl.MovementServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovementServiceImplTest {

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private MovementEventPublisher movementEventPublisher;

    private MovementServiceImpl movementService;

    @BeforeEach
    void setUp() {
        movementService = new MovementServiceImpl(movementRepository, new MovementMapper(), movementEventPublisher);
    }

    @Test
    void createMovement_shouldCreateMovement_whenValidRequest() {
        CreateMovementRequest request = new CreateMovementRequest(
                10L, 20L, MovementType.STOCK_IN, MovementDirection.IN,
                new BigDecimal("12.50"), new BigDecimal("5.00"), new BigDecimal("100.00"),
                ReferenceType.GRN, "500", "GRN-500", MovementReasonCode.PURCHASE_RECEIPT,
                "Received goods", LocalDateTime.now(), "movement-service", "corr-1");

        when(movementRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(movementRepository.existsByMovementNumber(any())).thenReturn(false);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setMovementId(1L);
            return movement;
        });

        var response = movementService.createMovement(request, 101L);

        assertNotNull(response.movementNumber());
        assertEquals(new BigDecimal("62.5000"), response.totalValue());
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void createMovement_shouldRejectNegativeQuantity() {
        CreateMovementRequest request = new CreateMovementRequest(
                10L, 20L, MovementType.STOCK_IN, MovementDirection.IN,
                new BigDecimal("-1.00"), new BigDecimal("5.00"), new BigDecimal("100.00"),
                ReferenceType.GRN, "500", "GRN-500", MovementReasonCode.PURCHASE_RECEIPT,
                "Received goods", LocalDateTime.now(), "movement-service", "corr-1");

        assertThrows(InvalidMovementException.class, () -> movementService.createMovement(request, 101L));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void getMovementById_shouldThrowNotFound() {
        when(movementRepository.findByMovementId(99L)).thenReturn(Optional.empty());

        assertThrows(MovementNotFoundException.class, () -> movementService.getMovementById(99L));
    }

    @Test
    void reverseMovement_shouldCreateOppositeMovement() {
        StockMovement original = StockMovement.builder()
                .movementId(5L)
                .movementNumber("MOV-20260501-000005")
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.STOCK_IN)
                .direction(MovementDirection.IN)
                .quantity(new BigDecimal("10.0000"))
                .unitCost(new BigDecimal("5.0000"))
                .totalValue(new BigDecimal("50.0000"))
                .balanceAfter(new BigDecimal("80.0000"))
                .referenceType(ReferenceType.GRN)
                .isReversal(false)
                .movementDate(LocalDateTime.now())
                .build();

        when(movementRepository.findByMovementId(5L)).thenReturn(Optional.of(original));
        when(movementRepository.existsByRelatedMovementId(5L)).thenReturn(false);
        when(movementRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(movementRepository.existsByMovementNumber(any())).thenReturn(false);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setMovementId(6L);
            return movement;
        });

        var response = movementService.reverseMovement(5L, new ReverseMovementRequest(MovementReasonCode.MANUAL_CORRECTION, "Duplicate GRN"), 200L);

        assertEquals(MovementType.REVERSAL, response.movementType());
        assertEquals(MovementDirection.OUT, response.direction());
        assertEquals(Long.valueOf(5L), response.relatedMovementId());
    }

    @Test
    void reverseMovement_shouldRejectAlreadyReversedMovement() {
        StockMovement original = StockMovement.builder()
                .movementId(5L)
                .movementNumber("MOV-20260501-000005")
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.STOCK_IN)
                .direction(MovementDirection.IN)
                .quantity(new BigDecimal("10.0000"))
                .unitCost(new BigDecimal("5.0000"))
                .totalValue(new BigDecimal("50.0000"))
                .balanceAfter(new BigDecimal("80.0000"))
                .isReversal(false)
                .movementDate(LocalDateTime.now())
                .build();

        when(movementRepository.findByMovementId(5L)).thenReturn(Optional.of(original));
        when(movementRepository.existsByRelatedMovementId(5L)).thenReturn(true);

        assertThrows(InvalidMovementException.class,
                () -> movementService.reverseMovement(5L, new ReverseMovementRequest(MovementReasonCode.MANUAL_CORRECTION, "Duplicate GRN"), 200L));
    }

    @Test
    void createMovementFromEvent_shouldSkipDuplicateEvent() {
        StockMovement existing = StockMovement.builder()
                .movementId(8L)
                .movementNumber("MOV-20260501-000008")
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.STOCK_IN)
                .direction(MovementDirection.IN)
                .quantity(new BigDecimal("5.0000"))
                .unitCost(BigDecimal.ZERO.setScale(4))
                .totalValue(BigDecimal.ZERO.setScale(4))
                .balanceAfter(new BigDecimal("30.0000"))
                .isReversal(false)
                .movementDate(LocalDateTime.now())
                .build();

        when(movementRepository.findByIdempotencyKey(eq("evt-1:STOCK_IN:20"))).thenReturn(Optional.of(existing));

        var response = movementService.createMovementFromEvent(new CreateMovementFromEventRequest(
                "evt-1", "STOCK_RECEIVED", 10L, null, null,
                20L, null, null, null, null,
                MovementType.STOCK_IN, MovementDirection.IN,
                new BigDecimal("5"), BigDecimal.ZERO, new BigDecimal("30"),
                ReferenceType.GRN, "500", "GRN-500", 99L, null,
                MovementReasonCode.PURCHASE_RECEIPT, "received", LocalDateTime.now(), "warehouse-service", "evt-1"));

        assertEquals(existing.getMovementNumber(), response.movementNumber());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void getMovementSummary_shouldReturnCorrectTotals() {
        when(movementRepository.findByMovementDateBetween(any(), any())).thenReturn(List.of(
                stockMovement(MovementType.STOCK_IN, new BigDecimal("10"), new BigDecimal("100")),
                stockMovement(MovementType.STOCK_OUT, new BigDecimal("4"), new BigDecimal("40")),
                stockMovement(MovementType.WRITE_OFF, new BigDecimal("1"), new BigDecimal("10"))));

        MovementSummaryResponse summary = movementService.getMovementSummary(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

        assertEquals(3, summary.totalMovements());
        assertEquals(new BigDecimal("10"), summary.totalStockInQuantity());
        assertEquals(new BigDecimal("5"), summary.totalStockOutQuantity());
    }

    private StockMovement stockMovement(MovementType type, BigDecimal quantity, BigDecimal value) {
        return StockMovement.builder()
                .movementId(System.nanoTime())
                .movementNumber("MOV")
                .productId(10L)
                .warehouseId(20L)
                .movementType(type)
                .direction(type == MovementType.STOCK_IN ? MovementDirection.IN : MovementDirection.OUT)
                .quantity(quantity)
                .unitCost(BigDecimal.ONE.setScale(4))
                .totalValue(value)
                .balanceAfter(new BigDecimal("100"))
                .isReversal(false)
                .movementDate(LocalDateTime.now())
                .build();
    }
}

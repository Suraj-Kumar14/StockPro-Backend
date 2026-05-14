package com.stockpro.movementservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.movementservice.dto.request.CreateMovementFromEventRequest;
import com.stockpro.movementservice.dto.request.CreateMovementRequest;
import com.stockpro.movementservice.dto.request.MovementSearchRequest;
import com.stockpro.movementservice.dto.request.ReverseMovementRequest;
import com.stockpro.movementservice.dto.response.MovementAnalyticsResponse;
import com.stockpro.movementservice.dto.response.MovementResponse;
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
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
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
    void createMovement_shouldRejectNegativeUnitCostAndInvalidDirection() {
        CreateMovementRequest negativeCost = new CreateMovementRequest(
                10L, 20L, MovementType.STOCK_IN, MovementDirection.IN,
                new BigDecimal("1.00"), new BigDecimal("-5.00"), new BigDecimal("100.00"),
                ReferenceType.GRN, "500", "GRN-500", MovementReasonCode.PURCHASE_RECEIPT,
                "Received goods", LocalDateTime.now(), "movement-service", "corr-1");
        CreateMovementRequest invalidDirection = new CreateMovementRequest(
                10L, 20L, MovementType.STOCK_IN, MovementDirection.OUT,
                new BigDecimal("1.00"), new BigDecimal("5.00"), new BigDecimal("100.00"),
                ReferenceType.GRN, "500", "GRN-500", MovementReasonCode.PURCHASE_RECEIPT,
                "Received goods", LocalDateTime.now(), "movement-service", "corr-2");

        assertThrows(InvalidMovementException.class, () -> movementService.createMovement(negativeCost, 101L));
        assertThrows(InvalidMovementException.class, () -> movementService.createMovement(invalidDirection, 101L));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void createMovement_shouldApplyDefaultsAndHandleMovementNumberCollision() {
        CreateMovementRequest request = new CreateMovementRequest(
                10L, 20L, MovementType.STOCK_OUT, MovementDirection.OUT,
                new BigDecimal("2.00"), null, new BigDecimal("8.00"),
                ReferenceType.SYSTEM, "   ", " REF-2 ", null,
                "   ", null, null, "   ");

        when(movementRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(movementRepository.existsByMovementNumber(any())).thenReturn(true, false);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setMovementId(2L);
            return movement;
        });

        var response = movementService.createMovement(request, 501L);

        assertEquals(MovementType.STOCK_OUT, response.movementType());
        assertEquals(MovementDirection.OUT, response.direction());
        assertEquals(new BigDecimal("0.0000"), response.unitCost());
        assertEquals(new BigDecimal("0.0000"), response.totalValue());
        assertEquals(MovementReasonCode.OTHER, response.reasonCode());
        assertEquals("movement-service", response.sourceService());
        verify(movementRepository, org.mockito.Mockito.times(2)).existsByMovementNumber(any());
        verify(movementEventPublisher).publish(eq("movement.stock-out"), any());
    }

    @Test
    void getMovementById_shouldThrowNotFound() {
        when(movementRepository.findByMovementId(99L)).thenReturn(Optional.empty());

        assertThrows(MovementNotFoundException.class, () -> movementService.getMovementById(99L));
    }

    @Test
    void getMovementById_shouldReturnMovementWhenPresent() {
        StockMovement movement = stockMovement(MovementType.STOCK_IN, new BigDecimal("2"), new BigDecimal("20"));
        movement.setMovementId(77L);
        movement.setMovementNumber("MOV-77");
        when(movementRepository.findByMovementId(77L)).thenReturn(Optional.of(movement));

        var response = movementService.getMovementById(77L);

        assertEquals(77L, response.movementId());
        assertEquals("MOV-77", response.movementNumber());
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
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        StockMovement reversal = captor.getValue();

        assertEquals(MovementType.ADJUSTMENT, response.movementType());
        assertEquals(MovementDirection.OUT, response.direction());
        assertEquals(Long.valueOf(5L), response.relatedMovementId());
        assertEquals(MovementType.ADJUSTMENT, reversal.getMovementType());
        assertEquals(MovementDirection.OUT, reversal.getDirection());
        assertEquals(original.getQuantity(), reversal.getQuantity());
        assertEquals(original.getBalanceAfter().subtract(original.getQuantity()), reversal.getBalanceAfter());
        assertEquals(MovementType.STOCK_IN, original.getMovementType());
        assertEquals(MovementDirection.IN, original.getDirection());
        assertNotEquals(Boolean.TRUE, original.getIsReversal());
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

        ReverseMovementRequest request = new ReverseMovementRequest(MovementReasonCode.MANUAL_CORRECTION, "Duplicate GRN");
        assertThrows(InvalidMovementException.class,
                () -> movementService.reverseMovement(5L, request, 200L));
    }

    @Test
    void reverseMovement_shouldThrowNotFoundWhenMovementDoesNotExist() {
        when(movementRepository.findByMovementId(404L)).thenReturn(Optional.empty());

        ReverseMovementRequest request = new ReverseMovementRequest(MovementReasonCode.MANUAL_CORRECTION, "missing");
        assertThrows(MovementNotFoundException.class,
                () -> movementService.reverseMovement(404L, request, 200L));
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

        when(movementRepository.findByIdempotencyKey("evt-1:STOCK_IN:20")).thenReturn(Optional.of(existing));

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

    @Test
    void listAndLookupMethods_shouldDelegateToRepositoryAndMapResults() {
        StockMovement movement = stockMovement(MovementType.STOCK_IN, new BigDecimal("2"), new BigDecimal("20"));
        when(movementRepository.findByMovementNumber("MOV-1")).thenReturn(Optional.of(movement));
        when(movementRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movement)));
        when(movementRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movement)));
        when(movementRepository.findByProductId(eq(10L), any())).thenReturn(new PageImpl<>(List.of(movement)));
        when(movementRepository.findByWarehouseId(eq(20L), any())).thenReturn(new PageImpl<>(List.of(movement)));
        when(movementRepository.findByReferenceTypeAndReferenceId(eq(ReferenceType.GRN), eq("500"), any()))
                .thenReturn(new PageImpl<>(List.of(movement)));
        when(movementRepository.findByPerformedBy(eq(101L), any())).thenReturn(new PageImpl<>(List.of(movement)));

        assertEquals(movement.getMovementNumber(), movementService.getMovementByNumber("MOV-1").movementNumber());
        assertEquals(1, movementService.getAllMovements(0, 10, "invalid", "asc").getTotalElements());
        assertEquals(1, movementService.searchMovements(MovementSearchRequest.builder()
                .keyword("MOV")
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.STOCK_IN)
                .direction(MovementDirection.IN)
                .referenceType(ReferenceType.GRN)
                .referenceId("500")
                .performedBy(101L)
                .fromDate(LocalDateTime.now().minusDays(1))
                .toDate(LocalDateTime.now().plusDays(1))
                .minQuantity(BigDecimal.ONE)
                .maxQuantity(new BigDecimal("10"))
                .sourceService("movement-service")
                .correlationId("corr")
                .isReversal(false)
                .page(0)
                .size(10)
                .sortBy("movementDate")
                .sortDir("desc")
                .build()).getTotalElements());
        assertEquals(1, movementService.getMovementsByProduct(10L, 0, 10).getTotalElements());
        assertEquals(1, movementService.getMovementsByWarehouse(20L, 0, 10).getTotalElements());
        assertEquals(1, movementService.getMovementsByReference("grn", "500", 0, 10).getTotalElements());
        assertEquals(1, movementService.getMovementsByUser(101L, 0, 10).getTotalElements());
    }

    @Test
    void createMovementFromEvent_shouldCreateTransferPair() {
        when(movementRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(movementRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L, 1L);
        when(movementRepository.existsByMovementNumber(any())).thenReturn(false);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setMovementId((long) (movement.getDirection() == MovementDirection.OUT ? 11 : 12));
            return movement;
        });

        var response = movementService.createMovementFromEvent(new CreateMovementFromEventRequest(
                "evt-transfer", "STOCK_TRANSFERRED", 10L, "SKU-10", "Widget",
                20L, "WH-20", "Main", 20L, 30L,
                null, null, new BigDecimal("5"), new BigDecimal("2"), new BigDecimal("50"),
                ReferenceType.TRANSFER, "500", "TR-500", 101L, "Alice",
                MovementReasonCode.TRANSFER_OUT, "transfer", LocalDateTime.now(), "warehouse-service", "corr-transfer"));

        assertEquals(MovementType.TRANSFER_OUT, response.movementType());
        verify(movementEventPublisher).publish(eq("movement.transfer-out"), any());
        verify(movementEventPublisher).publish(eq("movement.transfer-in"), any());
    }

    @Test
    void createMovementFromEvent_shouldRejectMissingIdsAndUnsupportedType() {
        CreateMovementFromEventRequest missingEventId = new CreateMovementFromEventRequest(
                null, "STOCK_RECEIVED", 10L, null, null,
                20L, null, null, null, null,
                MovementType.STOCK_IN, MovementDirection.IN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN,
                ReferenceType.GRN, "500", "GRN-500", 101L, null,
                MovementReasonCode.PURCHASE_RECEIPT, "received", LocalDateTime.now(), "warehouse-service", null);

        CreateMovementFromEventRequest unsupportedEvent = new CreateMovementFromEventRequest(
                "evt-x", "UNKNOWN_EVENT", 10L, null, null,
                20L, null, null, null, null,
                null, null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN,
                ReferenceType.GRN, "500", "GRN-500", 101L, null,
                MovementReasonCode.PURCHASE_RECEIPT, "received", LocalDateTime.now(), "warehouse-service", "corr");

        assertThrows(InvalidMovementException.class, () -> movementService.createMovementFromEvent(missingEventId));
        assertThrows(InvalidMovementException.class, () -> movementService.createMovementFromEvent(unsupportedEvent));
    }

    @Test
    void createMovementFromEvent_shouldSupportReservationReleaseAndRejectTransferWithoutWarehouses() {
        when(movementRepository.findByIdempotencyKey("evt-release:RESERVATION_RELEASE:20")).thenReturn(Optional.empty());
        when(movementRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(movementRepository.existsByMovementNumber(any())).thenReturn(false);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setMovementId(30L);
            return movement;
        });

        var response = movementService.createMovementFromEvent(new CreateMovementFromEventRequest(
                "evt-release", "STOCK_RESERVATION_RELEASED", 10L, "SKU-10", "Widget",
                20L, "WH-20", "Main", null, null,
                null, null, new BigDecimal("5"), new BigDecimal("2"), new BigDecimal("50"),
                ReferenceType.SYSTEM, "500", "SYS-500", 101L, "Alice",
                MovementReasonCode.OTHER, "reservation released", LocalDateTime.now(), "warehouse-service", "evt-release"));

        assertEquals(MovementType.RESERVATION_RELEASE, response.movementType());
        assertEquals(MovementDirection.NEUTRAL, response.direction());
        verify(movementEventPublisher).publish(eq("movement.created"), any());

        CreateMovementFromEventRequest missingWarehouses = new CreateMovementFromEventRequest(
                "evt-transfer", "STOCK_TRANSFERRED", 10L, "SKU-10", "Widget",
                20L, "WH-20", "Main", null, 30L,
                null, null, new BigDecimal("5"), new BigDecimal("2"), new BigDecimal("50"),
                ReferenceType.TRANSFER, "500", "TR-500", 101L, "Alice",
                MovementReasonCode.TRANSFER_OUT, "transfer", LocalDateTime.now(), "warehouse-service", "corr-transfer");

        assertThrows(InvalidMovementException.class, () -> movementService.createMovementFromEvent(missingWarehouses));
    }

    @Test
    void createMovementFromEvent_shouldUseCorrelationFallbackAndDefaultEventFields() {
        when(movementRepository.findByIdempotencyKey("corr-issue:STOCK_OUT:20")).thenReturn(Optional.empty());
        when(movementRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(movementRepository.existsByMovementNumber(any())).thenReturn(false);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setMovementId(41L);
            return movement;
        });

        var response = movementService.createMovementFromEvent(new CreateMovementFromEventRequest(
                null, "STOCK_ISSUED", 10L, " SKU-10 ", " Widget ",
                20L, " WH-20 ", " Main ", null, null,
                null, null, new BigDecimal("3"), null, new BigDecimal("7"),
                null, " REF-7 ", " REF-7 ", 900L, " Alice ",
                null, "  issued  ", null, null, " corr-issue "));

        assertEquals(MovementType.STOCK_OUT, response.movementType());
        assertEquals(MovementDirection.OUT, response.direction());
        assertEquals(ReferenceType.SYSTEM, response.referenceType());
        assertEquals(MovementReasonCode.OTHER, response.reasonCode());
        assertEquals(new BigDecimal("0.0000"), response.unitCost());
        assertEquals("warehouse-service", response.sourceService());
        verify(movementEventPublisher).publish(eq("movement.stock-out"), any());
    }

    @Test
    void createMovementFromEvent_shouldUseExplicitMovementTypeAndReturnRouting() {
        when(movementRepository.findByIdempotencyKey("evt-return:RETURN:20")).thenReturn(Optional.empty());
        when(movementRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(movementRepository.existsByMovementNumber(any())).thenReturn(false);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setMovementId(42L);
            return movement;
        });

        var response = movementService.createMovementFromEvent(new CreateMovementFromEventRequest(
                "evt-return", "IGNORED", 10L, null, null,
                20L, null, null, null, null,
                MovementType.RETURN, null, new BigDecimal("2"), BigDecimal.ONE, new BigDecimal("9"),
                ReferenceType.RETURN, "RET-1", "RET-1", 101L, null,
                MovementReasonCode.RETURNED, "returned", LocalDateTime.now(), "returns-service", null));

        assertEquals(MovementType.RETURN, response.movementType());
        assertEquals(MovementDirection.IN, response.direction());
        verify(movementEventPublisher).publish(eq("movement.returned"), any());
    }

    @Test
    void createMovementFromEvent_shouldReuseExistingTransferRecords() {
        StockMovement existingOut = namedMovement(
                "MOV-OUT", "Widget", "Source", MovementType.TRANSFER_OUT, MovementDirection.OUT,
                new BigDecimal("4"), new BigDecimal("40"));
        StockMovement existingIn = namedMovement(
                "MOV-IN", "Widget", "Destination", MovementType.TRANSFER_IN, MovementDirection.IN,
                new BigDecimal("4"), new BigDecimal("40"));

        when(movementRepository.findByIdempotencyKey("evt-transfer:TRANSFER_OUT:20")).thenReturn(Optional.of(existingOut));
        when(movementRepository.findByIdempotencyKey("evt-transfer:TRANSFER_IN:30")).thenReturn(Optional.of(existingIn));

        var response = movementService.createMovementFromEvent(new CreateMovementFromEventRequest(
                "evt-transfer", "STOCK_TRANSFERRED", 10L, "SKU-10", "Widget",
                20L, "WH-20", "Main", 20L, 30L,
                null, null, new BigDecimal("4"), null, null,
                null, "TR-1", "TR-1", 101L, "Alice",
                null, "duplicate transfer", null, null, null));

        assertEquals("MOV-OUT", response.movementNumber());
        verify(movementRepository, never()).save(any());
        verify(movementEventPublisher, never()).publish(eq("movement.transfer-out"), any());
    }

    @Test
    void reverseAndAnalyticsAndExport_shouldCoverAdditionalBranches() {
        ReverseMovementRequest missingReasonRequest = new ReverseMovementRequest(null, "missing reason");
        assertThrows(InvalidMovementException.class,
                () -> movementService.reverseMovement(1L, missingReasonRequest, 1L));

        when(movementRepository.findAll()).thenReturn(List.of(
                namedMovement("MOV-1", "Widget", "Main", MovementType.ADJUSTMENT, MovementDirection.IN, new BigDecimal("3"), new BigDecimal("30")),
                namedMovement("MOV-2", "Widget", "Main", MovementType.WRITE_OFF, MovementDirection.OUT, new BigDecimal("1"), new BigDecimal("10")),
                namedMovement("MOV-3", null, null, MovementType.STOCK_IN, MovementDirection.IN, new BigDecimal("5"), new BigDecimal("50"))));
        when(movementRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        namedMovement("MOV-1", "Widget", "Main", MovementType.ADJUSTMENT, MovementDirection.IN, new BigDecimal("3"), new BigDecimal("30")),
                        namedMovement("MOV-2", "Widget", "Main", MovementType.WRITE_OFF, MovementDirection.OUT, new BigDecimal("1"), new BigDecimal("10")),
                        namedMovement("MOV-3", null, null, MovementType.STOCK_IN, MovementDirection.IN, new BigDecimal("5"), new BigDecimal("50")))));

        MovementAnalyticsResponse analytics = movementService.getMovementAnalytics(null, null);
        byte[] csv = movementService.exportMovementsToCsv(MovementSearchRequest.builder()
                .page(0)
                .size(10)
                .sortBy("movementDate")
                .sortDir("desc")
                .build());

        assertEquals(3, analytics.movementCountByType().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(1, analytics.adjustmentTrend().size());
        assertEquals(1, analytics.writeOffTrend().size());
        assertThrows(InvalidMovementException.class, () -> movementService.getMovementsByReference("bad-ref", "1", 0, 10));
        assertEquals(4, new String(csv).lines().count());
    }

    @Test
    void reverseMovement_shouldRejectAlreadyMarkedAsReversal() {
        StockMovement original = StockMovement.builder()
                .movementId(10L)
                .movementNumber("MOV-20260501-000010")
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.REVERSAL)
                .direction(MovementDirection.NEUTRAL)
                .quantity(new BigDecimal("1.0000"))
                .unitCost(BigDecimal.ZERO.setScale(4))
                .totalValue(BigDecimal.ZERO.setScale(4))
                .balanceAfter(new BigDecimal("5.0000"))
                .isReversal(true)
                .movementDate(LocalDateTime.now())
                .build();

        when(movementRepository.findByMovementId(10L)).thenReturn(Optional.of(original));

        ReverseMovementRequest request = new ReverseMovementRequest(MovementReasonCode.MANUAL_CORRECTION, "already reversal");
        assertThrows(InvalidMovementException.class,
                () -> movementService.reverseMovement(10L, request, 200L));
    }

    @Test
    void reverseMovement_shouldRejectWhenReverseBalanceWouldGoNegative() {
        StockMovement original = StockMovement.builder()
                .movementId(9L)
                .movementNumber("MOV-20260501-000009")
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.STOCK_IN)
                .direction(MovementDirection.IN)
                .quantity(new BigDecimal("10.0000"))
                .unitCost(new BigDecimal("5.0000"))
                .totalValue(new BigDecimal("50.0000"))
                .balanceAfter(new BigDecimal("5.0000"))
                .referenceType(ReferenceType.GRN)
                .isReversal(false)
                .movementDate(LocalDateTime.now())
                .build();

        when(movementRepository.findByMovementId(9L)).thenReturn(Optional.of(original));
        when(movementRepository.existsByRelatedMovementId(9L)).thenReturn(false);

        ReverseMovementRequest request = new ReverseMovementRequest(MovementReasonCode.MANUAL_CORRECTION, "would go negative");
        assertThrows(InvalidMovementException.class,
                () -> movementService.reverseMovement(9L, request, 200L));
    }

    @Test
    void reverseMovement_shouldHandleOutboundMovement() {
        StockMovement original = StockMovement.builder()
                .movementId(11L)
                .movementNumber("MOV-20260501-000011")
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.STOCK_OUT)
                .direction(MovementDirection.OUT)
                .quantity(new BigDecimal("3.0000"))
                .unitCost(new BigDecimal("5.0000"))
                .totalValue(new BigDecimal("15.0000"))
                .balanceAfter(new BigDecimal("7.0000"))
                .referenceType(ReferenceType.SYSTEM)
                .isReversal(false)
                .movementDate(LocalDateTime.now())
                .build();

        when(movementRepository.findByMovementId(11L)).thenReturn(Optional.of(original));
        when(movementRepository.existsByRelatedMovementId(11L)).thenReturn(false);
        when(movementRepository.countByCreatedAtBetween(any(), any())).thenReturn(0L);
        when(movementRepository.existsByMovementNumber(any())).thenReturn(false);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setMovementId(12L);
            return movement;
        });

        var response = movementService.reverseMovement(
                11L, new ReverseMovementRequest(MovementReasonCode.MANUAL_CORRECTION, "restore stock"), 200L);

        assertEquals(MovementDirection.IN, response.direction());
        assertEquals(new BigDecimal("10.0000"), response.balanceAfter());
        verify(movementEventPublisher).publish(eq("movement.reversed"), any());
    }

    @Test
    void getMovementSummary_shouldUseRepositoryFindAllWhenNoRangeProvided() {
        when(movementRepository.findAll()).thenReturn(List.of(
                namedMovement("MOV-TIN", "Widget", "Main", MovementType.TRANSFER_IN, MovementDirection.IN, new BigDecimal("5"), new BigDecimal("50")),
                namedMovement("MOV-TOUT", "Widget", "Main", MovementType.TRANSFER_OUT, MovementDirection.OUT, new BigDecimal("5"), new BigDecimal("50")),
                namedMovement("MOV-RET", "Widget", "Main", MovementType.RETURN, MovementDirection.IN, new BigDecimal("2"), new BigDecimal("20")),
                namedMovement("MOV-CC", "Widget", "Main", MovementType.CYCLE_COUNT_CORRECTION, MovementDirection.NEUTRAL, new BigDecimal("1"), new BigDecimal("10"))));

        MovementSummaryResponse summary = movementService.getMovementSummary(null, null);

        assertEquals(new BigDecimal("10.0000"), summary.totalTransferQuantity());
        assertEquals(new BigDecimal("1.0000"), summary.totalAdjustmentQuantity());
        assertEquals(new BigDecimal("2.0000"), summary.totalReturnQuantity());
    }

    @Test
    void getRecentMovements_shouldClampRequestedLimitAndMapRepositoryResults() {
        StockMovement newest = namedMovement(
                "MOV-RECENT-1", "Widget", "Main", MovementType.STOCK_IN, MovementDirection.IN,
                new BigDecimal("2"), new BigDecimal("20"));
        StockMovement older = namedMovement(
                "MOV-RECENT-2", "Gadget", "Reserve", MovementType.STOCK_OUT, MovementDirection.OUT,
                new BigDecimal("1"), new BigDecimal("10"));

        when(movementRepository.findRecentMovements(10)).thenReturn(List.of(newest, older));

        List<MovementResponse> responses = movementService.getRecentMovements(99);

        assertEquals(2, responses.size());
        assertEquals("MOV-RECENT-1", responses.get(0).movementNumber());
        assertEquals("MOV-RECENT-2", responses.get(1).movementNumber());
        verify(movementRepository).findRecentMovements(10);
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

    private StockMovement namedMovement(String movementNumber, String productName, String warehouseName,
                                        MovementType type, MovementDirection direction, BigDecimal quantity, BigDecimal value) {
        return StockMovement.builder()
                .movementId(System.nanoTime())
                .movementNumber(movementNumber)
                .productId(10L)
                .productName(productName)
                .warehouseId(20L)
                .warehouseName(warehouseName)
                .movementType(type)
                .direction(direction)
                .quantity(quantity.setScale(4))
                .unitCost(BigDecimal.ONE.setScale(4))
                .totalValue(value.setScale(4))
                .balanceAfter(new BigDecimal("100.0000"))
                .referenceType(ReferenceType.GRN)
                .referenceId("500")
                .referenceNumber("GRN-500")
                .performedBy(101L)
                .reasonCode(MovementReasonCode.MANUAL_CORRECTION)
                .notes("note")
                .isReversal(false)
                .movementDate(LocalDateTime.now())
                .sourceService("movement-service")
                .correlationId("corr")
                .build();
    }
}

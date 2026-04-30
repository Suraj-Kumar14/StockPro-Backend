package com.stockpro.movementservice;

import com.stockpro.movementservice.dto.StockMovementRequestDTO;
import com.stockpro.movementservice.dto.StockMovementResponseDTO;
import com.stockpro.movementservice.entity.MovementType;
import com.stockpro.movementservice.entity.StockMovement;
import com.stockpro.movementservice.exception.InvalidMovementException;
import com.stockpro.movementservice.exception.MovementNotFoundException;
import com.stockpro.movementservice.exception.NegativeStockException;
import com.stockpro.movementservice.repository.StockMovementRepository;
import com.stockpro.movementservice.service.StockMovementMapper;
import com.stockpro.movementservice.service.StockMovementService;
import com.stockpro.movementservice.service.StockMovementValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private StockMovementRepository movementRepository;

    private StockMovementService movementService;

    private StockMovement existingMovement;
    private StockMovementRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        movementService = new StockMovementService(
                movementRepository,
                new StockMovementMapper(),
                new StockMovementValidationService());

        existingMovement = StockMovement.builder()
                .movementId(1L)
                .productId(1L)
                .warehouseId(1L)
                .movementType(MovementType.STOCK_IN)
                .quantity(100)
                .referenceId(1L)
                .referenceType("PO")
                .unitCost(new BigDecimal("50.00"))
                .performedBy(1L)
                .notes("Initial stock")
                .movementDate(LocalDateTime.now().minusDays(1))
                .balanceAfter(100)
                .build();

        requestDTO = new StockMovementRequestDTO();
        requestDTO.setProductId(1L);
        requestDTO.setWarehouseId(1L);
        requestDTO.setMovementType(MovementType.STOCK_IN);
        requestDTO.setQuantity(25);
        requestDTO.setReferenceId(2L);
        requestDTO.setReferenceType("PO");
        requestDTO.setUnitCost(new BigDecimal("55.00"));
        requestDTO.setPerformedBy(1L);
        requestDTO.setNotes("GRN receipt");
        requestDTO.setBalanceAfter(125);
    }

    @Test
    void recordMovement_ComputesBalanceFromLedger() {
        StockMovement savedMovement = StockMovement.builder()
                .movementId(2L)
                .productId(1L)
                .warehouseId(1L)
                .movementType(MovementType.STOCK_IN)
                .quantity(25)
                .referenceId(2L)
                .referenceType("PO")
                .unitCost(new BigDecimal("55.00"))
                .performedBy(1L)
                .notes("GRN receipt")
                .balanceAfter(125)
                .movementDate(LocalDateTime.now())
                .build();

        when(movementRepository.findLatestBalanceCandidates(eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(List.of(existingMovement));
        when(movementRepository.save(any(StockMovement.class))).thenReturn(savedMovement);

        StockMovementResponseDTO result = movementService.recordMovement(requestDTO);

        assertNotNull(result);
        assertEquals(125, result.getBalanceAfter());
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void recordMovement_BalanceMismatch_ThrowsException() {
        requestDTO.setBalanceAfter(999);
        when(movementRepository.findLatestBalanceCandidates(eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(List.of(existingMovement));

        assertThrows(InvalidMovementException.class,
                () -> movementService.recordMovement(requestDTO));
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    void recordMovement_StockOutCausingNegativeBalance_ThrowsException() {
        requestDTO.setMovementType(MovementType.STOCK_OUT);
        requestDTO.setQuantity(150);
        requestDTO.setReferenceType("ISSUE");
        requestDTO.setBalanceAfter(-50);

        when(movementRepository.findLatestBalanceCandidates(eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(List.of(existingMovement));

        assertThrows(NegativeStockException.class,
                () -> movementService.recordMovement(requestDTO));
    }

    @Test
    void recordMovement_AdjustmentAllowsNegativeQuantityWithReason() {
        requestDTO.setMovementType(MovementType.ADJUSTMENT);
        requestDTO.setQuantity(-10);
        requestDTO.setReferenceType("ADJUSTMENT");
        requestDTO.setNotes("Cycle count correction");
        requestDTO.setBalanceAfter(90);

        StockMovement savedMovement = StockMovement.builder()
                .movementId(3L)
                .productId(1L)
                .warehouseId(1L)
                .movementType(MovementType.ADJUSTMENT)
                .quantity(-10)
                .referenceId(2L)
                .referenceType("ADJUSTMENT")
                .performedBy(1L)
                .notes("Cycle count correction")
                .balanceAfter(90)
                .movementDate(LocalDateTime.now())
                .build();

        when(movementRepository.findLatestBalanceCandidates(eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(List.of(existingMovement));
        when(movementRepository.save(any(StockMovement.class))).thenReturn(savedMovement);

        StockMovementResponseDTO result = movementService.recordMovement(requestDTO);

        assertEquals(90, result.getBalanceAfter());
        assertEquals(MovementType.ADJUSTMENT, result.getMovementType());
    }

    @Test
    void recordMovement_WriteOffRequiresReason() {
        requestDTO.setMovementType(MovementType.WRITE_OFF);
        requestDTO.setQuantity(5);
        requestDTO.setReferenceType("WRITE_OFF");
        requestDTO.setNotes(" ");

        assertThrows(InvalidMovementException.class,
                () -> movementService.recordMovement(requestDTO));
    }

    @Test
    void recordTransferPair_RecordsOutAndInAtomically() {
        StockMovementRequestDTO transferOut = new StockMovementRequestDTO();
        transferOut.setProductId(1L);
        transferOut.setWarehouseId(1L);
        transferOut.setMovementType(MovementType.TRANSFER_OUT);
        transferOut.setQuantity(10);
        transferOut.setReferenceId(50L);
        transferOut.setReferenceType("TRANSFER");
        transferOut.setPerformedBy(1L);
        transferOut.setNotes("Transfer to WH-2");
        transferOut.setBalanceAfter(90);

        StockMovementRequestDTO transferIn = new StockMovementRequestDTO();
        transferIn.setProductId(1L);
        transferIn.setWarehouseId(2L);
        transferIn.setMovementType(MovementType.TRANSFER_IN);
        transferIn.setQuantity(10);
        transferIn.setReferenceId(50L);
        transferIn.setReferenceType("TRANSFER");
        transferIn.setPerformedBy(1L);
        transferIn.setNotes("Transfer from WH-1");
        transferIn.setBalanceAfter(10);

        StockMovement outSaved = StockMovement.builder()
                .movementId(10L).productId(1L).warehouseId(1L)
                .movementType(MovementType.TRANSFER_OUT).quantity(10)
                .referenceId(50L).referenceType("TRANSFER").performedBy(1L)
                .notes("Transfer to WH-2").balanceAfter(90).movementDate(LocalDateTime.now()).build();
        StockMovement inSaved = StockMovement.builder()
                .movementId(11L).productId(1L).warehouseId(2L)
                .movementType(MovementType.TRANSFER_IN).quantity(10)
                .referenceId(50L).referenceType("TRANSFER").performedBy(1L)
                .notes("Transfer from WH-1").balanceAfter(10).movementDate(LocalDateTime.now()).build();

        when(movementRepository.findLatestBalanceCandidates(eq(1L), eq(1L), any(Pageable.class)))
                .thenReturn(List.of(existingMovement));
        when(movementRepository.findLatestBalanceCandidates(eq(1L), eq(2L), any(Pageable.class)))
                .thenReturn(List.of());
        when(movementRepository.save(any(StockMovement.class))).thenReturn(outSaved, inSaved);

        List<StockMovementResponseDTO> result = movementService.recordTransferPair(transferOut, transferIn);

        assertEquals(2, result.size());
        assertEquals(MovementType.TRANSFER_OUT, result.get(0).getMovementType());
        assertEquals(MovementType.TRANSFER_IN, result.get(1).getMovementType());
    }

    @Test
    void getMovementById_NotFound_ThrowsException() {
        when(movementRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(MovementNotFoundException.class,
                () -> movementService.getMovementById(99L));
    }

    @Test
    void getMovementHistory_ReturnsChronologicalList() {
        StockMovement laterMovement = StockMovement.builder()
                .movementId(2L)
                .productId(1L)
                .warehouseId(1L)
                .movementType(MovementType.STOCK_OUT)
                .quantity(20)
                .referenceId(3L)
                .referenceType("ISSUE")
                .performedBy(2L)
                .movementDate(LocalDateTime.now())
                .balanceAfter(80)
                .build();

        when(movementRepository.findByProductIdAndWarehouseIdOrderByMovementDateAscMovementIdAsc(1L, 1L))
                .thenReturn(List.of(existingMovement, laterMovement));

        List<StockMovementResponseDTO> result = movementService.getMovementHistory(1L, 1L);

        assertEquals(2, result.size());
        assertEquals(existingMovement.getMovementId(), result.get(0).getMovementId());
        assertEquals(laterMovement.getMovementId(), result.get(1).getMovementId());
    }

    @Test
    void getTotalStockInAndOut_AggregatesByDirection() {
        StockMovement outboundMovement = StockMovement.builder()
                .movementId(2L)
                .productId(1L)
                .warehouseId(1L)
                .movementType(MovementType.STOCK_OUT)
                .quantity(30)
                .referenceId(4L)
                .referenceType("ISSUE")
                .performedBy(1L)
                .movementDate(LocalDateTime.now())
                .balanceAfter(70)
                .build();

        when(movementRepository.findByProductIdAndWarehouseIdOrderByMovementDateAscMovementIdAsc(1L, 1L))
                .thenReturn(List.of(existingMovement, outboundMovement));

        assertEquals(100, movementService.getTotalStockIn(1L, 1L));
        assertEquals(30, movementService.getTotalStockOut(1L, 1L));
    }
}

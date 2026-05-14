package com.stockpro.movementservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockpro.movementservice.entity.StockMovement;
import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementReasonCode;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class StockMovementRepositoryTest {

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        stockMovementRepository.save(movement("MOV-001", 10L, 20L, MovementType.STOCK_IN, "500", null, "evt-1"));
        stockMovementRepository.save(movement("MOV-002", 10L, 21L, MovementType.STOCK_OUT, "501", 1L, "evt-2"));
        stockMovementRepository.save(movement("MOV-003", 11L, 20L, MovementType.ADJUSTMENT, "502", null, "evt-3"));
    }

    @Test
    void shouldFindMovementsByRepositoryFilters() {
        assertTrue(stockMovementRepository.findByMovementId(1L).isPresent());
        assertTrue(stockMovementRepository.findByMovementNumber("MOV-001").isPresent());
        assertTrue(stockMovementRepository.existsByMovementNumber("MOV-002"));
        assertEquals(2, stockMovementRepository.findByProductId(10L, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(2, stockMovementRepository.findByWarehouseId(20L, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, stockMovementRepository.findByMovementType(MovementType.STOCK_OUT, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, stockMovementRepository.findByReferenceTypeAndReferenceId(ReferenceType.GRN, "500", PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void shouldReturnCountsAndIdempotencyLookups() {
        assertEquals(1L, stockMovementRepository.countByMovementType(MovementType.ADJUSTMENT));
        assertEquals(2L, stockMovementRepository.countByWarehouseId(20L));
        assertEquals(2L, stockMovementRepository.countByProductId(10L));
        assertEquals(3L, stockMovementRepository.countByCreatedAtBetween(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)));
        assertTrue(stockMovementRepository.existsByRelatedMovementId(1L));
        assertTrue(stockMovementRepository.findByIdempotencyKey("evt-2").isPresent());
        assertEquals(3, stockMovementRepository.findByMovementDateBetween(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1)).size());
    }

    @Test
    void shouldReturnRecentMovementsUsingDefaultRepositoryMethod() {
        var recentMovements = stockMovementRepository.findRecentMovements(2);

        assertEquals(2, recentMovements.size());
        assertTrue(recentMovements.get(0).getMovementDate().compareTo(recentMovements.get(1).getMovementDate()) >= 0);
    }

    @Test
    void shouldPersistMovementTypeValuesThatMatchEnumNames() {
        stockMovementRepository.deleteAll();
        int index = 1;
        for (MovementType type : MovementType.values()) {
            stockMovementRepository.save(movement("MOV-TYPE-" + index, 100L + index, 200L + index, type, "REF-" + index, null, "type-" + index));
            index++;
        }

        Set<String> persistedTypes = jdbcTemplate.queryForList(
                        "select distinct movement_type from stock_movements",
                        String.class)
                .stream()
                .collect(Collectors.toSet());
        Set<String> enumNames = Arrays.stream(MovementType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(enumNames, persistedTypes);
    }

    private StockMovement movement(String number, Long productId, Long warehouseId, MovementType type, String referenceId, Long relatedMovementId, String idempotencyKey) {
        return StockMovement.builder()
                .movementNumber(number)
                .productId(productId)
                .warehouseId(warehouseId)
                .movementType(type)
                .direction(directionFor(type))
                .quantity(new BigDecimal("5.0000"))
                .unitCost(new BigDecimal("2.0000"))
                .totalValue(new BigDecimal("10.0000"))
                .balanceAfter(new BigDecimal("20.0000"))
                .referenceType(ReferenceType.GRN)
                .referenceId(referenceId)
                .performedBy(1L)
                .reasonCode(MovementReasonCode.PURCHASE_RECEIPT)
                .movementDate(LocalDateTime.now())
                .relatedMovementId(relatedMovementId)
                .idempotencyKey(idempotencyKey)
                .isReversal(false)
                .build();
    }

    private MovementDirection directionFor(MovementType type) {
        return switch (type) {
            case STOCK_IN, TRANSFER_IN -> MovementDirection.IN;
            case STOCK_OUT, TRANSFER_OUT, WRITE_OFF -> MovementDirection.OUT;
            case RESERVATION, RESERVATION_RELEASE, REVERSAL -> MovementDirection.NEUTRAL;
            case ADJUSTMENT, RETURN, CYCLE_COUNT_CORRECTION -> MovementDirection.IN;
        };
    }
}

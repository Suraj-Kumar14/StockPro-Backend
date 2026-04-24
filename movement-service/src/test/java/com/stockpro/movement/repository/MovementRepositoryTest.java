package com.stockpro.movement.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.enums.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class MovementRepositoryTest {

    @Autowired
    private MovementRepository movementRepository;

    @Test
    void findByProductIdAndWarehouseIdOrderByMovementDateAscMovementIdAscShouldReturnAscendingHistory() {
        movementRepository.save(buildMovement(101L, 1L, MovementType.STOCK_IN, "10.0000", "10.0000",
                LocalDateTime.of(2026, 4, 1, 10, 0)));
        movementRepository.save(buildMovement(101L, 1L, MovementType.STOCK_OUT, "2.0000", "8.0000",
                LocalDateTime.of(2026, 4, 2, 10, 0)));

        List<StockMovement> history =
                movementRepository.findByProductIdAndWarehouseIdOrderByMovementDateAscMovementIdAsc(101L, 1L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getMovementType()).isEqualTo(MovementType.STOCK_IN);
        assertThat(history.get(1).getMovementType()).isEqualTo(MovementType.STOCK_OUT);
    }

    @Test
    void sumQuantityByProductIdAndMovementTypesShouldReturnExpectedTotal() {
        movementRepository.save(buildMovement(200L, 3L, MovementType.STOCK_IN, "15.5000", "15.5000",
                LocalDateTime.of(2026, 4, 1, 9, 0)));
        movementRepository.save(buildMovement(200L, 3L, MovementType.RETURN, "4.5000", "20.0000",
                LocalDateTime.of(2026, 4, 2, 9, 0)));
        movementRepository.save(buildMovement(200L, 3L, MovementType.STOCK_OUT, "3.0000", "17.0000",
                LocalDateTime.of(2026, 4, 3, 9, 0)));

        BigDecimal totalStockIn = movementRepository.sumQuantityByProductIdAndMovementTypes(
                200L, EnumSet.of(MovementType.STOCK_IN, MovementType.RETURN));

        assertThat(totalStockIn).isEqualByComparingTo("20.0000");
    }

    private StockMovement buildMovement(Long productId,
            Long warehouseId,
            MovementType movementType,
            String quantity,
            String balanceAfter,
            LocalDateTime movementDate) {
        return StockMovement.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .movementType(movementType)
                .quantity(new BigDecimal(quantity))
                .referenceId(500L)
                .referenceType("TEST")
                .unitCost(new BigDecimal("12.5000"))
                .performedBy(1L)
                .notes("Repository test")
                .movementDate(movementDate)
                .balanceAfter(new BigDecimal(balanceAfter))
                .createdBy("repo@test.com")
                .sourceService("TEST")
                .build();
    }
}

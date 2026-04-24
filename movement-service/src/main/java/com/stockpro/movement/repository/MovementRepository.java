package com.stockpro.movement.repository;

import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.enums.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    List<StockMovement> findByProductId(Long productId);

    List<StockMovement> findByWarehouseId(Long warehouseId);

    List<StockMovement> findByMovementType(MovementType movementType);

    List<StockMovement> findByReferenceId(Long referenceId);

    List<StockMovement> findByPerformedBy(Long performedBy);

    List<StockMovement> findByMovementDateBetween(LocalDateTime start, LocalDateTime end);

    List<StockMovement> findByProductIdAndWarehouseIdOrderByMovementDateAscMovementIdAsc(Long productId, Long warehouseId);

    List<StockMovement> findByProductIdAndWarehouseIdOrderByMovementDateDescMovementIdDesc(Long productId, Long warehouseId);

    @Query("""
            select coalesce(sum(m.quantity), 0)
            from StockMovement m
            where m.productId = :productId
              and m.movementType in :movementTypes
            """)
    BigDecimal sumQuantityByProductIdAndMovementTypes(@Param("productId") Long productId,
            @Param("movementTypes") Collection<MovementType> movementTypes);

    @Query("""
            select m
            from StockMovement m
            where (:productId is null or m.productId = :productId)
              and (:warehouseId is null or m.warehouseId = :warehouseId)
              and (:movementType is null or m.movementType = :movementType)
              and (:startDate is null or m.movementDate >= :startDate)
              and (:endDate is null or m.movementDate <= :endDate)
            order by m.movementDate asc, m.movementId asc
            """)
    List<StockMovement> findMovementHistory(@Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("movementType") MovementType movementType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

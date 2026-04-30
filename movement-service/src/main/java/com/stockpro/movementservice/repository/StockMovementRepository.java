package com.stockpro.movementservice.repository;

import com.stockpro.movementservice.entity.MovementType;
import com.stockpro.movementservice.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductId(Long productId);

    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    List<StockMovement> findByWarehouseId(Long warehouseId);

    Page<StockMovement> findByWarehouseId(Long warehouseId, Pageable pageable);

    List<StockMovement> findByMovementType(MovementType movementType);

    Page<StockMovement> findByMovementType(MovementType movementType, Pageable pageable);

    List<StockMovement> findByReferenceId(Long referenceId);

    List<StockMovement> findByPerformedBy(Long performedBy);

    List<StockMovement> findByProductIdAndWarehouseId(
            Long productId, Long warehouseId);

    List<StockMovement> findByMovementDateBetween(
            LocalDateTime start, LocalDateTime end);

    List<StockMovement> findByProductIdAndMovementType(
            Long productId, MovementType movementType);

    // Total stock in for a product in a warehouse
    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m " +
           "WHERE m.productId = :productId " +
           "AND m.warehouseId = :warehouseId " +
           "AND m.movementType = 'STOCK_IN'")
    Integer getTotalStockIn(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // Total stock out for a product in a warehouse
    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m " +
           "WHERE m.productId = :productId " +
           "AND m.warehouseId = :warehouseId " +
           "AND m.movementType = 'STOCK_OUT'")
    Integer getTotalStockOut(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    // History for a specific product in a specific warehouse ordered by date
    List<StockMovement> findByProductIdAndWarehouseIdOrderByMovementDateDesc(
            Long productId, Long warehouseId);

    List<StockMovement> findByProductIdAndWarehouseIdOrderByMovementDateAscMovementIdAsc(
            Long productId, Long warehouseId);

    List<StockMovement> findAllByOrderByMovementDateDescMovementIdDesc();

    @Query("""
            SELECT m
            FROM StockMovement m
            WHERE m.productId = :productId
              AND m.warehouseId = :warehouseId
            ORDER BY m.movementDate DESC, m.movementId DESC
            """)
    List<StockMovement> findLatestBalanceCandidates(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            Pageable pageable);
}

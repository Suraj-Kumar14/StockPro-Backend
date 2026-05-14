package com.stockpro.movementservice.repository;

import com.stockpro.movementservice.entity.StockMovement;
import com.stockpro.movementservice.enums.MovementType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    Optional<StockMovement> findByMovementId(Long movementId);

    Optional<StockMovement> findByMovementNumber(String movementNumber);

    boolean existsByMovementNumber(String movementNumber);

    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    Page<StockMovement> findByWarehouseId(Long warehouseId, Pageable pageable);

    Page<StockMovement> findByMovementType(MovementType movementType, Pageable pageable);

    Page<StockMovement> findByReferenceTypeAndReferenceId(
            com.stockpro.movementservice.enums.ReferenceType referenceType,
            String referenceId,
            Pageable pageable);

    Page<StockMovement> findByPerformedBy(Long performedBy, Pageable pageable);

    List<StockMovement> findByMovementDateBetween(LocalDateTime from, LocalDateTime to);

    long countByMovementType(MovementType movementType);

    long countByWarehouseId(Long warehouseId);

    long countByProductId(Long productId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByRelatedMovementId(Long relatedMovementId);

    Optional<StockMovement> findByIdempotencyKey(String idempotencyKey);

    List<StockMovement> findTop10ByOrderByMovementDateDesc();

    default List<StockMovement> findRecentMovements(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(limit, 1), Sort.by(Sort.Direction.DESC, "movementDate"));
        return findAll(pageable).getContent();
    }
}

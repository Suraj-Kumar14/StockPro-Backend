package com.stockpro.warehouseservice.repository;

import com.stockpro.warehouseservice.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<StockMovement> findByWarehouseIdAndProductIdOrderByCreatedAtDesc(
            Long warehouseId, Long productId);
}

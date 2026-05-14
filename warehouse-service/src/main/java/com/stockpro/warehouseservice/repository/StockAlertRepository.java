package com.stockpro.warehouseservice.repository;

import com.stockpro.warehouseservice.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    Optional<StockAlert> findByAlertIdAndActiveTrue(Long alertId);

    Optional<StockAlert> findByWarehouseIdAndProductIdAndAlertTypeAndActiveTrue(
            Long warehouseId, Long productId, String alertType);

    List<StockAlert> findByActiveTrueOrderByCreatedAtDesc();
}

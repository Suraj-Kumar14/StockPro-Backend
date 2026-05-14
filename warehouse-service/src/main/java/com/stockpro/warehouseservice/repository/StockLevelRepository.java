package com.stockpro.warehouseservice.repository;

import com.stockpro.warehouseservice.entity.StockLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockLevelRepository extends JpaRepository<StockLevel, Long>, JpaSpecificationExecutor<StockLevel> {

    Optional<StockLevel> findByWarehouseIdAndProductId(
            Long warehouseId, Long productId);

    List<StockLevel> findByWarehouseId(Long warehouseId);

    Page<StockLevel> findByWarehouseId(Long warehouseId, Pageable pageable);

    List<StockLevel> findByProductId(Long productId);

    Page<StockLevel> findByProductId(Long productId, Pageable pageable);

    @Query("""
            SELECT s
            FROM StockLevel s
            WHERE (s.quantity - s.reservedQuantity) < COALESCE(s.reorderLevel, :defaultReorderLevel)
            """)
    List<StockLevel> findLowStockItems(
            @Param("defaultReorderLevel") Integer defaultReorderLevel);

    // All stock entries where available quantity is 0 or less
    @Query("SELECT s FROM StockLevel s WHERE (s.quantity - s.reservedQuantity) <= 0")
    List<StockLevel> findOutOfStockItems();

    boolean existsByWarehouseIdAndProductId(Long warehouseId, Long productId);

    long countByWarehouseId(Long warehouseId);

    List<StockLevel> findByQuantityLessThanEqual(Integer quantity);

    List<StockLevel> findByQuantityGreaterThanEqual(Integer quantity);

    @Query("""
            SELECT COALESCE(SUM(s.quantity), 0)
            FROM StockLevel s
            WHERE s.warehouseId = :warehouseId
            """)
    Integer sumQuantityByWarehouseId(@Param("warehouseId") Long warehouseId);
}

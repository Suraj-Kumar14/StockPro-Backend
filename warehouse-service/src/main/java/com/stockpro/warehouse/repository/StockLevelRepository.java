package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.entity.StockLevel;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long>, JpaSpecificationExecutor<StockLevel> {

    Optional<StockLevel> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<StockLevel> findByWarehouseId(Long warehouseId);

    List<StockLevel> findByProductId(Long productId);

    Page<StockLevel> findByWarehouseId(Long warehouseId, Pageable pageable);

    Page<StockLevel> findByProductId(Long productId, Pageable pageable);

    List<StockLevel> findByProductIdIn(Collection<Long> productIds);

    @Query("select coalesce(sum(s.quantity), 0) from StockLevel s where s.warehouseId = :warehouseId")
    BigDecimal sumQuantityByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("""
            select s.warehouseId as warehouseId, coalesce(sum(s.quantity), 0) as totalQuantity
            from StockLevel s
            where s.warehouseId in :warehouseIds
            group by s.warehouseId
            """)
    List<WarehouseQuantityProjection> sumQuantityByWarehouseIds(@Param("warehouseIds") Collection<Long> warehouseIds);
}

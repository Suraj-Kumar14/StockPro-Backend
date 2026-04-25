package com.stockpro.report.repository;

import com.stockpro.report.entity.InventorySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<InventorySnapshot, Long> {

    List<InventorySnapshot> findByWarehouseId(Long warehouseId);

    List<InventorySnapshot> findByProductId(Long productId);

    List<InventorySnapshot> findBySnapshotDate(LocalDate snapshotDate);

    List<InventorySnapshot> findBySnapshotDateBetween(LocalDate fromDate, LocalDate toDate);

    List<InventorySnapshot> findByWarehouseIdAndSnapshotDateBetween(Long warehouseId, LocalDate fromDate, LocalDate toDate);

    List<InventorySnapshot> findByProductIdAndSnapshotDateBetween(Long productId, LocalDate fromDate, LocalDate toDate);

    boolean existsByWarehouseIdAndProductIdAndSnapshotDate(Long warehouseId, Long productId, LocalDate snapshotDate);

    boolean existsBySnapshotDate(LocalDate snapshotDate);

    Optional<InventorySnapshot> findFirstByWarehouseIdAndProductIdOrderBySnapshotDateDescCreatedAtDesc(
            Long warehouseId,
            Long productId);

    @Query("""
            select coalesce(sum(s.stockValue), 0)
            from InventorySnapshot s
            where s.snapshotDate = :snapshotDate
            """)
    BigDecimal sumStockValueBySnapshotDate(@Param("snapshotDate") LocalDate snapshotDate);

    @Query("""
            select s.warehouseId as warehouseId,
                   max(s.warehouseName) as warehouseName,
                   coalesce(sum(s.stockValue), 0) as totalStockValue
            from InventorySnapshot s
            where s.snapshotDate = :snapshotDate
            group by s.warehouseId
            order by s.warehouseId
            """)
    List<WarehouseStockValueProjection> sumStockValueByWarehouse(@Param("snapshotDate") LocalDate snapshotDate);

    @Query("""
            select s.productId as productId,
                   coalesce(avg(s.stockValue), 0) as averageStockValue
            from InventorySnapshot s
            where s.snapshotDate between :fromDate and :toDate
            group by s.productId
            """)
    List<ProductAverageStockValueProjection> averageInventoryValueByProductAndDateRange(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            select s
            from InventorySnapshot s
            where s.snapshotDate = (
                select max(innerSnapshot.snapshotDate)
                from InventorySnapshot innerSnapshot
                where (:warehouseId is null or innerSnapshot.warehouseId = :warehouseId)
                  and (:productId is null or innerSnapshot.productId = :productId)
            )
              and (:warehouseId is null or s.warehouseId = :warehouseId)
              and (:productId is null or s.productId = :productId)
            """)
    List<InventorySnapshot> findLatestSnapshots(@Param("warehouseId") Long warehouseId, @Param("productId") Long productId);

    @Query("""
            select max(s.snapshotDate)
            from InventorySnapshot s
            where (:requestedDate is null or s.snapshotDate <= :requestedDate)
            """)
    LocalDate findLatestSnapshotDateOnOrBefore(@Param("requestedDate") LocalDate requestedDate);

    @Query("""
            select s
            from InventorySnapshot s
            where s.snapshotDate = :snapshotDate
              and (:warehouseId is null or s.warehouseId = :warehouseId)
              and (:productId is null or s.productId = :productId)
            """)
    List<InventorySnapshot> findBySnapshotDateAndFilters(@Param("snapshotDate") LocalDate snapshotDate,
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId);
}

package com.stockpro.purchaseservice.repository;

import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    @Query("select po from PurchaseOrder po where po.poId = :purchaseOrderId")
    Optional<PurchaseOrder> findByPurchaseOrderId(Long purchaseOrderId);

    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    boolean existsByPoNumber(String poNumber);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByWarehouseId(Long warehouseId);

    List<PurchaseOrder> findByStatus(POStatus status);

    List<PurchaseOrder> findAllByStatusIn(Collection<POStatus> statuses);

    List<PurchaseOrder> findByCreatedById(Long createdById);

    List<PurchaseOrder> findByOrderDateBetween(
            LocalDate startDate, LocalDate endDate);

    List<PurchaseOrder> findBySupplierIdAndStatus(
            Long supplierId, POStatus status);

    long countByStatus(POStatus status);

    Page<PurchaseOrder> findByStatus(POStatus status, Pageable pageable);

    Page<PurchaseOrder> findBySupplierId(Long supplierId, Pageable pageable);

    Page<PurchaseOrder> findByWarehouseId(Long warehouseId, Pageable pageable);

    Page<PurchaseOrder> findByCreatedById(Long createdById, Pageable pageable);

    Page<PurchaseOrder> findByOrderDateBetween(
            LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("""
            select po
            from PurchaseOrder po
            where po.expectedDate < :today
              and po.status in :statuses
            """)
    List<PurchaseOrder> findOverduePurchaseOrders(
            @Param("statuses") Collection<POStatus> statuses,
            @Param("today") LocalDate today);

    List<PurchaseOrder> findByStatusAndExpectedDateBefore(POStatus status, LocalDate date);
}

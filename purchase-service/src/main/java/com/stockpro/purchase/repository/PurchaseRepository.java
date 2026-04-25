package com.stockpro.purchase.repository;

import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.entity.PurchaseOrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByWarehouseId(Long warehouseId);

    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);

    Optional<PurchaseOrder> findByPoId(Long poId);

    List<PurchaseOrder> findByOrderDateBetween(LocalDate start, LocalDate end);

    List<PurchaseOrder> findByCreatedById(Long createdById);

    List<PurchaseOrder> findByStatusAndExpectedDateBeforeAndReceivedDateIsNull(PurchaseOrderStatus status, LocalDate expectedDate);

    long countByStatus(PurchaseOrderStatus status);
}

package com.stockpro.purchaseservice.repository;

import com.stockpro.purchaseservice.entity.PurchaseOrderHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderHistoryRepository extends JpaRepository<PurchaseOrderHistory, Long> {
    List<PurchaseOrderHistory> findByPurchaseOrderIdOrderByActionAtAsc(Long purchaseOrderId);
}

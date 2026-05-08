package com.stockpro.paymentservice.repository;

import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(Long paymentId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    boolean existsByPaymentNumber(String paymentNumber);

    boolean existsByPurchaseOrderIdAndStatusIn(Long purchaseOrderId, Collection<PaymentStatus> statuses);

    Page<Payment> findByPurchaseOrderId(Long purchaseOrderId, Pageable pageable);

    @Query("""
            select coalesce(sum(p.paymentAmount), 0)
            from Payment p
            where p.purchaseOrderId = :purchaseOrderId
              and p.status in :statuses
            """)
    BigDecimal sumPaymentAmountByPurchaseOrderIdAndStatusIn(
            @Param("purchaseOrderId") Long purchaseOrderId,
            @Param("statuses") Collection<PaymentStatus> statuses);
}

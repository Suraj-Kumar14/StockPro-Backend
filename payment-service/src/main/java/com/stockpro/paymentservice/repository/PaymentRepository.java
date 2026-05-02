package com.stockpro.paymentservice.repository;

import com.stockpro.paymentservice.entity.Payment;
import com.stockpro.paymentservice.enums.PaymentStatus;
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
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByPaymentId(Long paymentId);

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    boolean existsByPaymentNumber(String paymentNumber);

    Page<Payment> findByPurchaseOrderId(Long purchaseOrderId, Pageable pageable);

    Page<Payment> findBySupplierId(Long supplierId, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Page<Payment> findByCreatedBy(Long createdBy, Pageable pageable);

    List<Payment> findByPurchaseOrderId(Long purchaseOrderId);

    long countByStatus(PaymentStatus status);

    @Query("""
            select coalesce(sum(p.paymentAmount), 0)
            from Payment p
            where p.status in :statuses
            """)
    BigDecimal sumPaymentAmountByStatusIn(@Param("statuses") Collection<PaymentStatus> statuses);

    @Query("""
            select coalesce(sum(p.remainingAmount), 0)
            from Payment p
            where p.status not in :excludedStatuses
            """)
    BigDecimal sumRemainingAmountExcludingStatuses(@Param("excludedStatuses") Collection<PaymentStatus> excludedStatuses);

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

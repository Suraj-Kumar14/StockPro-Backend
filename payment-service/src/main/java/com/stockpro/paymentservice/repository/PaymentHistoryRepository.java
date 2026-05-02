package com.stockpro.paymentservice.repository;

import com.stockpro.paymentservice.entity.PaymentHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    List<PaymentHistory> findByPaymentPaymentIdOrderByActionAtAsc(Long paymentId);
}

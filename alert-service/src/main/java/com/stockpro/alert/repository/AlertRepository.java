package com.stockpro.alert.repository;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    List<Alert> findByRecipientId(Long recipientId);

    List<Alert> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Alert> findByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    List<Alert> findByRecipientIdAndIsReadOrderByCreatedAtDesc(Long recipientId, Boolean isRead);

    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    List<Alert> findByType(AlertType type);

    List<Alert> findBySeverity(AlertSeverity severity);

    List<Alert> findByRelatedProductId(Long productId);

    List<Alert> findByRelatedWarehouseId(Long warehouseId);

    List<Alert> findByIsAcknowledged(Boolean isAcknowledged);

    List<Alert> findByRecipientIdAndIsAcknowledged(Long recipientId, Boolean isAcknowledged);

    List<Alert> findByRecipientIdAndIsAcknowledgedOrderByCreatedAtDesc(Long recipientId, Boolean isAcknowledged);

    void deleteByAlertId(Long alertId);
}

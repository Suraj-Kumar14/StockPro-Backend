package com.stockpro.alertservice.repository;

import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.entity.AlertType;
import com.stockpro.alertservice.entity.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByRecipientIdAndIsArchivedFalse(Long recipientId);

    List<Alert> findByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    List<Alert> findByRecipientIdAndIsAcknowledged(Long recipientId, Boolean isAcknowledged);

    List<Alert> findByRecipientIdAndIsReadAndIsArchivedFalse(Long recipientId, Boolean isRead);

    List<Alert> findByRecipientIdAndIsAcknowledgedAndIsArchivedFalse(Long recipientId, Boolean isAcknowledged);

    List<Alert> findByTypeAndIsArchivedFalse(AlertType type);

    List<Alert> findBySeverityAndIsArchivedFalse(Severity severity);

    List<Alert> findByRelatedProductIdAndIsArchivedFalse(Long productId);

    List<Alert> findByRelatedWarehouseIdAndIsArchivedFalse(Long warehouseId);

    Long countByRecipientIdAndIsReadAndIsArchivedFalse(Long recipientId, Boolean isRead);

    Long countByIsAcknowledgedAndIsArchivedFalse(Boolean isAcknowledged);

    Page<Alert> findByRecipientIdAndIsArchivedFalse(Long recipientId, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.recipientId = :recipientId AND a.isRead = false AND a.isArchived = false ORDER BY a.createdAt DESC")
    List<Alert> findUnreadAlertsByRecipient(@Param("recipientId") Long recipientId);

    @Query("SELECT a FROM Alert a WHERE a.recipientId = :recipientId AND a.isAcknowledged = false AND a.severity = 'CRITICAL' AND a.isArchived = false ORDER BY a.createdAt DESC")
    List<Alert> findUnacknowledgedCriticalAlerts(@Param("recipientId") Long recipientId);

    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :startDate AND a.isArchived = false ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT a FROM Alert a WHERE a.isAcknowledged = false AND a.isArchived = false ORDER BY a.createdAt DESC")
    List<Alert> findUnacknowledgedAlerts();
}

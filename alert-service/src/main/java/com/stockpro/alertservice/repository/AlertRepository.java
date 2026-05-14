package com.stockpro.alertservice.repository;

import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    Optional<Alert> findByAlertId(Long alertId);

    Optional<Alert> findByAlertNumber(String alertNumber);

    boolean existsByAlertNumber(String alertNumber);

    boolean existsByCorrelationIdAndTypeAndRecipientId(String correlationId, AlertType type, Long recipientId);

    boolean existsByCorrelationIdAndTypeAndRecipientRole(String correlationId, AlertType type, String recipientRole);

    Page<Alert> findByRecipientId(Long recipientId, Pageable pageable);

    Page<Alert> findByRecipientRole(String recipientRole, Pageable pageable);

    Page<Alert> findByRecipientIdAndIsReadFalse(Long recipientId, Pageable pageable);

    Page<Alert> findByRecipientIdAndIsAcknowledgedFalse(Long recipientId, Pageable pageable);

    Page<Alert> findByType(AlertType type, Pageable pageable);

    Page<Alert> findBySeverity(AlertSeverity severity, Pageable pageable);

    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);

    List<Alert> findByExpiresAtBeforeAndStatusNot(LocalDateTime now, AlertStatus status);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    long countByRecipientRoleAndIsReadFalse(String recipientRole);

    long countBySeverity(AlertSeverity severity);

    long countByType(AlertType type);

    Optional<Alert> findTopByAlertNumberStartingWithOrderByAlertNumberDesc(String prefix);

    @Modifying
    @Query("update Alert a set a.isArchived = true where a.type in :types and (a.isArchived = false or a.isArchived is null)")
    int archiveByTypes(@Param("types") List<AlertType> types);

    @Modifying
    @Query("update Alert a set a.isArchived = false where a.type in :types and a.isArchived = true")
    int unarchiveByTypes(@Param("types") List<AlertType> types);

    @Modifying
    @Query("update Alert a set a.type = :targetType where a.type = :sourceType")
    int replaceAlertType(@Param("sourceType") AlertType sourceType, @Param("targetType") AlertType targetType);

    @Modifying
    @Query("update Alert a set a.recipientRole = :targetRole where upper(a.recipientRole) = upper(:sourceRole)")
    int replaceRecipientRole(@Param("sourceRole") String sourceRole, @Param("targetRole") String targetRole);
}

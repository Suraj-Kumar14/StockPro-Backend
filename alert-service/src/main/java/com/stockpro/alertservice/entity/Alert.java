package com.stockpro.alertservice.entity;

import com.stockpro.alertservice.enums.AlertChannel;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_number", columnList = "alertNumber", unique = true),
        @Index(name = "idx_alert_recipient_id", columnList = "recipientId"),
        @Index(name = "idx_alert_recipient_role", columnList = "recipientRole"),
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_created_at", columnList = "createdAt"),
        @Index(name = "idx_alert_correlation", columnList = "correlationId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;

    @Column(nullable = false, unique = true, length = 32)
    private String alertNumber;

    private Long recipientId;

    @Column(length = 50)
    private String recipientRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertChannel channel;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(length = 2000)
    private String userMessage;

    @Column(length = 2000)
    private String technicalDetails;

    private Long relatedProductId;
    private Long relatedWarehouseId;
    private Long relatedPurchaseOrderId;
    private Long relatedSupplierId;
    private Long relatedMovementId;

    @Column(length = 50)
    private String referenceType;

    @Column(length = 100)
    private String referenceId;

    @Column(length = 100)
    private String referenceNumber;

    @Column(nullable = false)
    private Boolean isRead;

    @Column(nullable = false)
    private Boolean isAcknowledged;

    @Column(nullable = false)
    private Boolean isDismissed;

    @Column(nullable = false)
    private Boolean isArchived;

    private LocalDateTime readAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime dismissedAt;
    private Long acknowledgedBy;
    private Long dismissedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    @Column(length = 50)
    private String sourceService;

    @Column(length = 120)
    private String correlationId;

    private Integer priority;

    @Column(length = 255)
    private String actionUrl;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (status == null) {
            status = AlertStatus.NEW;
        }
        if (channel == null) {
            channel = AlertChannel.IN_APP;
        }
        if (isRead == null) {
            isRead = false;
        }
        if (isAcknowledged == null) {
            isAcknowledged = false;
        }
        if (isDismissed == null) {
            isDismissed = false;
        }
        if (isArchived == null) {
            isArchived = false;
        }
    }
}

package com.stockpro.alert.entity;

import com.stockpro.alert.enums.AlertChannel;
import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_recipient", columnList = "recipient_id"),
        @Index(name = "idx_alert_recipient_read", columnList = "recipient_id,is_read"),
        @Index(name = "idx_alert_type", columnList = "type"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_product", columnList = "related_product_id"),
        @Index(name = "idx_alert_warehouse", columnList = "related_warehouse_id"),
        @Index(name = "idx_alert_po", columnList = "related_purchase_order_id"),
        @Index(name = "idx_alert_acknowledged", columnList = "is_acknowledged"),
        @Index(name = "idx_alert_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 50)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 50)
    private AlertSeverity severity;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "related_product_id")
    private Long relatedProductId;

    @Column(name = "related_warehouse_id")
    private Long relatedWarehouseId;

    @Column(name = "related_purchase_order_id")
    private Long relatedPurchaseOrderId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private AlertChannel channel;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "is_acknowledged", nullable = false)
    private Boolean isAcknowledged;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    void onCreate() {
        if (isRead == null) {
            isRead = Boolean.FALSE;
        }
        if (isAcknowledged == null) {
            isAcknowledged = Boolean.FALSE;
        }
        if (channel == null) {
            channel = AlertChannel.IN_APP;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

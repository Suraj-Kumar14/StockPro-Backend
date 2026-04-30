package com.stockpro.alertservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_recipient", columnList = "recipientId"),
        @Index(name = "idx_alert_is_read", columnList = "isRead"),
        @Index(name = "idx_alert_is_acknowledged", columnList = "isAcknowledged"),
        @Index(name = "idx_alert_type", columnList = "type"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long recipientId; // User ID who receives this alert

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column
    private Long relatedProductId; // Optional: link to product

    @Column
    private Long relatedWarehouseId; // Optional: link to warehouse

    @Column
    private Long relatedPurchaseOrderId; // Optional: link to PO

    @Column(length = 50)
    private String channel = "IN_APP"; // IN_APP, EMAIL, SMS

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Boolean isAcknowledged = false;

    @Column
    private LocalDateTime readAt;

    @Column
    private LocalDateTime acknowledgedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean isArchived = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (channel == null) {
            channel = "IN_APP";
        }
        if (isRead == null) {
            isRead = false;
        }
        if (isAcknowledged == null) {
            isAcknowledged = false;
        }
        if (isArchived == null) {
            isArchived = false;
        }
    }
}

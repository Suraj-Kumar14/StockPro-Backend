package com.stockpro.purchaseservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "purchase_order_history", indexes = {
        @Index(name = "idx_po_history_po_id", columnList = "purchaseOrderId"),
        @Index(name = "idx_po_history_action_at", columnList = "actionAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @Column(nullable = false)
    private Long purchaseOrderId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 50)
    private String oldStatus;

    @Column(length = 50)
    private String newStatus;

    private Long actorId;

    @Column(length = 1000)
    private String remarks;

    @Column(nullable = false, updatable = false)
    private LocalDateTime actionAt;

    @PrePersist
    protected void onCreate() {
        actionAt = LocalDateTime.now();
    }
}

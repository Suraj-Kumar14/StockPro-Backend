package com.stockpro.purchaseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders", indexes = {
        @Index(name = "idx_po_status", columnList = "status"),
        @Index(name = "idx_po_supplier", columnList = "supplierId"),
        @Index(name = "idx_po_warehouse", columnList = "warehouseId"),
        @Index(name = "idx_po_order_date", columnList = "orderDate"),
        @Index(name = "idx_po_expected_date", columnList = "expectedDate"),
        @Index(name = "idx_po_reference_number", columnList = "referenceNumber", unique = true),
        @Index(name = "idx_po_po_number", columnList = "poNumber", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long poId;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Long createdById;

    private Long approvedBy;

    private Long rejectedBy;

    private Long cancelledBy;

    private Long receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private POStatus status = POStatus.DRAFT;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(updatable = false)
    private LocalDate orderDate;

    private LocalDate expectedDate;

    private LocalDate receivedDate;

    private LocalDate actualDeliveryDate;

    @Column(length = 500)
    private String notes;

    private String paymentTerms;

    @Column(length = 500)
    private String approvalRemarks;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 500)
    private String cancellationReason;

    @Column(unique = true, length = 50)
    private String referenceNumber;

    @Column(unique = true, length = 50)
    private String poNumber;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "purchaseOrder",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.EAGER)
    @Builder.Default
    private List<POLineItem> lineItems = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        orderDate = LocalDate.now();
        if (status == null) {
            status = POStatus.DRAFT;
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
        if (subtotalAmount == null) {
            subtotalAmount = BigDecimal.ZERO;
        }
        if (taxAmount == null) {
            taxAmount = BigDecimal.ZERO;
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
        if (shippingAmount == null) {
            shippingAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public Long getPurchaseOrderId() {
        return poId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.poId = purchaseOrderId;
    }

    @Transient
    public Long getCreatedBy() {
        return createdById;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdById = createdBy;
    }

    @Transient
    public LocalDate getExpectedDeliveryDate() {
        return expectedDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDate = expectedDeliveryDate;
    }

    @Transient
    public LocalDate getActualDeliveryDate() {
        return actualDeliveryDate != null ? actualDeliveryDate : receivedDate;
    }

    public void setActualDeliveryDate(LocalDate actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }
}

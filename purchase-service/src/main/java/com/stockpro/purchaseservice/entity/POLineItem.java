package com.stockpro.purchaseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "po_line_items", indexes = {
        @Index(name = "idx_po_line_po_id", columnList = "purchase_order_id"),
        @Index(name = "idx_po_line_product_id", columnList = "product_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class POLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_item_id")
    private Long lineItemId;

    @Version
    private Long version;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    private String productSku;

    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "received_qty", nullable = false)
    @Builder.Default
    private Integer receivedQty = 0;

    private String notes;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @ToString.Exclude
    private PurchaseOrder purchaseOrder;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (receivedQty == null) {
            receivedQty = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public Long getPurchaseOrderId() {
        return purchaseOrder != null ? purchaseOrder.getPoId() : null;
    }

    @Transient
    public Integer getOrderedQuantity() {
        return quantity;
    }

    public void setOrderedQuantity(Integer orderedQuantity) {
        this.quantity = orderedQuantity;
    }

    @Transient
    public Integer getReceivedQuantity() {
        return receivedQty;
    }

    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQty = receivedQuantity;
    }

    @Transient
    public BigDecimal getLineTotal() {
        return totalCost;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.totalCost = lineTotal;
    }
}

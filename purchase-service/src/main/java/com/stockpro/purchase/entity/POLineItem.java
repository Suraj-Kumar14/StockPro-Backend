package com.stockpro.purchase.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "po_line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class POLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lineItemId;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCost;

    @Column(nullable = false)
    private Integer receivedQty;

    @PrePersist
    public void applyDefaults() {
        if (receivedQty == null) {
            receivedQty = 0;
        }
        if (totalCost == null) {
            totalCost = BigDecimal.ZERO;
        }
    }
}

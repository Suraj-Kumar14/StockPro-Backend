package com.stockpro.purchaseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "po_line_items", indexes = {
        @Index(name = "idx_po_line_po_id", columnList = "purchase_order_id"),
        @Index(name = "idx_po_line_product_id", columnList = "productId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class POLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lineItemId;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(nullable = false)
    @Builder.Default
    private Integer receivedQty = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @ToString.Exclude
    private PurchaseOrder purchaseOrder;
}

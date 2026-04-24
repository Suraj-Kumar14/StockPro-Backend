package com.stockpro.warehouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_levels", uniqueConstraints = {
        @UniqueConstraint(name = "idx_stock_warehouse_product", columnNames = { "warehouse_id", "product_id" })
}, indexes = {
        @Index(name = "idx_stock_product", columnList = "product_id"),
        @Index(name = "idx_stock_last_updated", columnList = "last_updated")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "reserved_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedQuantity;

    @Column(length = 255)
    private String location;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Transient
    public BigDecimal getAvailableQuantity() {
        BigDecimal onHand = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal reserved = reservedQuantity == null ? BigDecimal.ZERO : reservedQuantity;
        return onHand.subtract(reserved);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (quantity == null) {
            quantity = BigDecimal.ZERO;
        }
        if (reservedQuantity == null) {
            reservedQuantity = BigDecimal.ZERO;
        }
        if (lastUpdated == null) {
            lastUpdated = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        LocalDateTime now = LocalDateTime.now();
        lastUpdated = now;
        updatedAt = now;
    }
}

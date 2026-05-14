package com.stockpro.reportservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_snapshots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_snapshot_date_product_warehouse",
                        columnNames = {"snapshotDate", "productId", "warehouseId"})
        },
        indexes = {
        @Index(name = "idx_snapshot_date", columnList = "snapshotDate"),
        @Index(name = "idx_snapshot_warehouse", columnList = "warehouseId"),
        @Index(name = "idx_snapshot_product", columnList = "productId"),
        @Index(name = "idx_snapshot_date_product_warehouse", columnList = "snapshotDate, productId, warehouseId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long snapshotId;

    @NotNull
    @Column(nullable = false)
    private LocalDate snapshotDate;

    @NotNull
    @Column(nullable = false)
    private Long productId;

    private String productSku;

    private String productName;

    @NotNull
    @Column(nullable = false)
    private Long warehouseId;

    private String warehouseCode;

    private String warehouseName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal availableQuantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalValue;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (snapshotDate == null) {
            snapshotDate = LocalDate.now();
        }
    }
}

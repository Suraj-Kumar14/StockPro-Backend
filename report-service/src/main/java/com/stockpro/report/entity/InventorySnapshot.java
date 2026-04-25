package com.stockpro.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "inventory_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_snapshot_wh_product_date",
                columnNames = { "warehouse_id", "product_id", "snapshot_date" }),
        indexes = {
                @Index(name = "idx_snapshot_warehouse", columnList = "warehouse_id"),
                @Index(name = "idx_snapshot_product", columnList = "product_id"),
                @Index(name = "idx_snapshot_date", columnList = "snapshot_date"),
                @Index(name = "idx_snapshot_warehouse_date", columnList = "warehouse_id, snapshot_date"),
                @Index(name = "idx_snapshot_product_date", columnList = "product_id, snapshot_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long snapshotId;

    @NotNull
    @Column(nullable = false)
    private Long warehouseId;

    @NotNull
    @Column(nullable = false)
    private Long productId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal stockValue;

    @NotNull
    @Column(nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String productSku;

    @Column(length = 255)
    private String productName;

    @Column(length = 255)
    private String warehouseName;

    @Column(precision = 19, scale = 4)
    private BigDecimal costPrice;

    @Column(length = 100)
    private String source;

    @Column(length = 255)
    private String createdBy;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

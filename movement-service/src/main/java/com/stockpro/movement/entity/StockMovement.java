package com.stockpro.movement.entity;

import com.stockpro.movement.enums.MovementType;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_movement_product", columnList = "product_id"),
        @Index(name = "idx_movement_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_movement_type", columnList = "movement_type"),
        @Index(name = "idx_movement_reference", columnList = "reference_id"),
        @Index(name = "idx_movement_date", columnList = "movement_date"),
        @Index(name = "idx_movement_performed_by", columnList = "performed_by")
})
@Immutable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movement_id", nullable = false, updatable = false)
    private Long movementId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50, updatable = false)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal quantity;

    @Column(name = "reference_id", updatable = false)
    private Long referenceId;

    @Column(name = "reference_type", length = 100, updatable = false)
    private String referenceType;

    @Column(name = "unit_cost", precision = 19, scale = 4, updatable = false)
    private BigDecimal unitCost;

    @Column(name = "performed_by", nullable = false, updatable = false)
    private Long performedBy;

    @Column(name = "notes", length = 1000, updatable = false)
    private String notes;

    @Column(name = "movement_date", nullable = false, updatable = false)
    private LocalDateTime movementDate;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255, updatable = false)
    private String createdBy;

    @Column(name = "source_service", length = 100, updatable = false)
    private String sourceService;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

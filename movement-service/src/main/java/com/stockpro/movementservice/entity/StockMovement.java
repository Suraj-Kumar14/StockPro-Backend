package com.stockpro.movementservice.entity;

import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementReasonCode;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "stock_movements",
        indexes = {
                @Index(name = "idx_movement_number", columnList = "movement_number", unique = true),
                @Index(name = "idx_movement_product", columnList = "product_id"),
                @Index(name = "idx_movement_warehouse", columnList = "warehouse_id"),
                @Index(name = "idx_movement_type", columnList = "movement_type"),
                @Index(name = "idx_movement_reference", columnList = "reference_type, reference_id"),
                @Index(name = "idx_movement_performed_by", columnList = "performed_by"),
                @Index(name = "idx_movement_date", columnList = "movement_date"),
                @Index(name = "idx_movement_related", columnList = "related_movement_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_movement_idempotency_key", columnNames = "idempotency_key")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long movementId;

    @Column(name = "movement_number", nullable = false, length = 40, unique = true)
    private String movementNumber;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_sku", length = 100)
    private String productSku;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "warehouse_code", length = 100)
    private String warehouseCode;

    @Column(name = "warehouse_name", length = 255)
    private String warehouseName;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private MovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private MovementDirection direction;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "total_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalValue;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 40)
    private ReferenceType referenceType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "performed_by_name", length = 150)
    private String performedByName;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 40)
    private MovementReasonCode reasonCode;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "related_movement_id")
    private Long relatedMovementId;

    @Column(name = "is_reversal", nullable = false)
    private Boolean isReversal;

    @Column(name = "movement_date", nullable = false)
    private LocalDateTime movementDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "source_service", length = 100)
    private String sourceService;

    @Column(name = "correlation_id", length = 150)
    private String correlationId;

    @Column(name = "source_event_id", length = 150)
    private String sourceEventId;

    @Column(name = "idempotency_key", length = 200)
    private String idempotencyKey;

    @Version
    private Long version;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (movementDate == null) {
            movementDate = createdAt;
        }
        if (isReversal == null) {
            isReversal = Boolean.FALSE;
        }
        if (sourceService == null || sourceService.isBlank()) {
            sourceService = "movement-service";
        }
        if (unitCost == null) {
            unitCost = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        } else {
            unitCost = unitCost.setScale(4, RoundingMode.HALF_UP);
        }
        if (quantity != null) {
            quantity = quantity.setScale(4, RoundingMode.HALF_UP);
        }
        if (balanceAfter != null) {
            balanceAfter = balanceAfter.setScale(4, RoundingMode.HALF_UP);
        }
        if (totalValue == null && quantity != null && unitCost != null) {
            totalValue = quantity.multiply(unitCost).setScale(4, RoundingMode.HALF_UP);
        }
    }
}

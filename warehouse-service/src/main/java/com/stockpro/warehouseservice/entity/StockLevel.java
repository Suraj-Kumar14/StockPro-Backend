package com.stockpro.warehouseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_levels", uniqueConstraints = @UniqueConstraint(columnNames = { "warehouse_id", "product_id" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLevel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long stockId;

	@Version
	private Long version;

	@Column(nullable = false)
	private Long warehouseId;

	// Product ID from product-service
	@Column(nullable = false)
	private Long productId;

	@Column(nullable = false)
	@Builder.Default
	private Integer quantity = 0;

	@Column(nullable = false)
	@Builder.Default
	private Integer reservedQuantity = 0;

	private Integer reorderLevel;

	private Integer maxStockLevel;

	// bin/aisle reference
	@Column(length = 50)
	private String binLocation;

	private LocalDateTime lastUpdated;

	
	public Integer getAvailableQuantity() {
		return defaultIfNull(quantity) - defaultIfNull(reservedQuantity);
	}

	@PrePersist
	@PreUpdate
	protected void onUpdate() {
		lastUpdated = LocalDateTime.now();
		quantity = defaultIfNull(quantity);
		reservedQuantity = defaultIfNull(reservedQuantity);
	}

	private int defaultIfNull(Integer value) {
		return value == null ? 0 : value;
	}
}

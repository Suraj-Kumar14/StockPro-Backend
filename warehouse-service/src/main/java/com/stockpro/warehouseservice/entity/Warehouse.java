package com.stockpro.warehouseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long warehouseId;

	@Version
	private Long version;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@Column(nullable = false, length = 200)
	private String location;

	@Column(length = 300)
	private String address;

	@Column(length = 100)
	private String city;

	@Column(length = 100)
	private String state;

	@Column(length = 100)
	private String country;

	// ID of the manager user (from auth-service)
	private Long managerId;

	@Column(nullable = false)
	private Integer capacity;

	@Column(nullable = false)
	@Builder.Default
	private Integer usedCapacity = 0;

	@Column(length = 20)
	private String phone;

	@Column(nullable = false)
	@Builder.Default
	private Boolean isActive = true;

	@Column(updatable = false)
	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private Long createdBy;

	private Long updatedBy;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = createdAt;
		if (isActive == null) {
			isActive = true;
		}
		if (usedCapacity == null) {
			usedCapacity = 0;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
		if (usedCapacity == null) {
			usedCapacity = 0;
		}
		if (isActive == null) {
			isActive = true;
		}
	}
}

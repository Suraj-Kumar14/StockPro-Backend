package com.stockpro.supplierservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers", indexes = {
        @Index(name = "idx_supplier_name", columnList = "name"),
        @Index(name = "idx_supplier_city", columnList = "city"),
        @Index(name = "idx_supplier_country", columnList = "country"),
        @Index(name = "idx_supplier_email", columnList = "email", unique = true),
        @Index(name = "idx_supplier_tax_id", columnList = "taxId", unique = true),
        @Index(name = "idx_supplier_active", columnList = "isActive")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplierId;

    @Version
    private Long version;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String contactPerson;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(unique = true, length = 50)
    private String taxId; // GST/VAT/Tax Registration Number

    @Column(length = 50)
    private String paymentTerms; // NET-30, NET-60, NET-90, etc.

    @Column(nullable = false)
    private Integer leadTimeDays; // Days from order to delivery

    @Column
    private Double rating;

    @Column
    private Integer totalOrders = 0; // Count of POs placed

    @Column(nullable = false)
    private Integer ratingCount = 0;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (rating == null) {
            rating = 0.0;
        }
        if (totalOrders == null) {
            totalOrders = 0;
        }
        if (ratingCount == null) {
            ratingCount = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.stockpro.supplierservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers", indexes = {
        @Index(name = "idx_supplier_name", columnList = "name"),
        @Index(name = "idx_supplier_city", columnList = "city"),
        @Index(name = "idx_supplier_country", columnList = "country"),
        @Index(name = "idx_supplier_email", columnList = "email", unique = true),
        @Index(name = "idx_supplier_tax_id", columnList = "taxId", unique = true),
        @Index(name = "idx_supplier_code", columnList = "supplierCode", unique = true),
        @Index(name = "idx_supplier_status", columnList = "status"),
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

    @Column(unique = true, length = 50)
    private String supplierCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String contactPerson;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 50)
    private String alternatePhone;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(length = 20)
    private String postalCode;

    @Column(unique = true, length = 50)
    private String taxId; // GST/VAT/Tax Registration Number

    @Column(length = 50)
    private String gstNumber;

    @Column(length = 50)
    private String paymentTerms; // NET-30, NET-60, NET-90, etc.

    @Column(nullable = false)
    private Integer leadTimeDays; // Days from order to delivery

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @Column(precision = 4, scale = 2)
    private BigDecimal rating;

    @Column
    @Builder.Default
    private Integer totalOrders = 0; // Count of POs placed

    @Column(nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(length = 1000)
    private String notes;

    private Long createdBy;

    private Long updatedBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (rating == null) {
            rating = BigDecimal.ZERO;
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
        if (status == null) {
            status = SupplierStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public String getTaxNumber() {
        return taxId;
    }

    public void setTaxNumber(String taxNumber) {
        this.taxId = taxNumber;
    }
}

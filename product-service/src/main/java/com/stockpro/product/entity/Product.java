package com.stockpro.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

import lombok.*;

/**
 * Product master entity.
 * This table stores product details and threshold values, not live warehouse quantity.
 */
@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_sku", columnNames = "sku"),
        @UniqueConstraint(name = "uk_product_barcode", columnNames = "barcode")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 30)
    private String unitOfMeasure;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal costPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    private Integer reorderLevel;

    @Column(nullable = false)
    private Integer maxStockLevel;

    @Column(nullable = false)
    private Integer leadTimeDays;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false, length = 100)
    private String barcode;

    @PrePersist
    public void setDefaultActiveValue() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }
}

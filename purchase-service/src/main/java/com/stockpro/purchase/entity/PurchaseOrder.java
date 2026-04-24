package com.stockpro.purchase.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long poId;

    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Long createdById;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseOrderStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDate orderDate;

    private LocalDate expectedDate;

    private LocalDate receivedDate;

    @Column(length = 1500)
    private String notes;

    @Column(length = 100)
    private String referenceNumber;

    @Builder.Default
    @JsonManagedReference
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<POLineItem> lineItems = new ArrayList<>();

    @PrePersist
    public void applyDefaults() {
        if (status == null) {
            status = PurchaseOrderStatus.DRAFT;
        }
        if (orderDate == null) {
            orderDate = LocalDate.now();
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }
}

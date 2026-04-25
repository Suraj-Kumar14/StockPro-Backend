package com.stockpro.purchase.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.purchase.client.ProductClient;
import com.stockpro.purchase.client.SupplierClient;
import com.stockpro.purchase.client.WarehouseClient;
import com.stockpro.purchase.dto.ProductSummaryResponse;
import com.stockpro.purchase.dto.SupplierSummaryResponse;
import com.stockpro.purchase.dto.WarehouseSummaryResponse;
import com.stockpro.purchase.dto.event.PoPendingApprovalEvent;
import com.stockpro.purchase.entity.POLineItem;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.entity.PurchaseOrderStatus;
import com.stockpro.purchase.publisher.AlertEventPublisher;
import com.stockpro.purchase.repository.PurchaseRepository;
import com.stockpro.purchase.security.JwtService;
import com.stockpro.purchase.service.impl.PurchaseServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceImplTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private SupplierClient supplierClient;

    @Mock
    private WarehouseClient warehouseClient;

    @Mock
    private AlertEventPublisher alertEventPublisher;

    @Mock
    private JwtService jwtService;

    private PurchaseServiceImpl purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseServiceImpl(
                purchaseRepository,
                productClient,
                supplierClient,
                warehouseClient,
                alertEventPublisher,
                jwtService);
    }

    @Test
    void createPoShouldPublishPendingApprovalEventWhenStatusRequiresApproval() {
        PurchaseOrder request = purchaseOrder(PurchaseOrderStatus.PENDING_APPROVAL);

        when(supplierClient.getSupplierById(7L)).thenReturn(SupplierSummaryResponse.builder()
                .supplierId(7L)
                .supplierName("ABC Supplies")
                .isActive(Boolean.TRUE)
                .build());
        when(productClient.getProductById(501L)).thenReturn(ProductSummaryResponse.builder()
                .productId(501L)
                .sku("SKU-501")
                .name("Industrial Drill")
                .costPrice(new BigDecimal("120.50"))
                .isActive(Boolean.TRUE)
                .build());
        when(warehouseClient.getWarehouseById(3L)).thenReturn(WarehouseSummaryResponse.builder()
                .warehouseId(3L)
                .name("Central Warehouse")
                .managerId(101L)
                .isActive(Boolean.TRUE)
                .build());
        when(purchaseRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder saved = invocation.getArgument(0);
            saved.setPoId(9001L);
            return saved;
        });

        PurchaseOrder saved = purchaseService.createPO(request);

        assertThat(saved.getPoId()).isEqualTo(9001L);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("1205.00");
        verify(alertEventPublisher).publishPoPendingApprovalEvent(any(PoPendingApprovalEvent.class));
    }

    @Test
    void createPoShouldNotPublishPendingApprovalEventWhenStatusIsDraft() {
        PurchaseOrder request = purchaseOrder(PurchaseOrderStatus.DRAFT);

        when(supplierClient.getSupplierById(7L)).thenReturn(SupplierSummaryResponse.builder()
                .supplierId(7L)
                .supplierName("ABC Supplies")
                .isActive(Boolean.TRUE)
                .build());
        when(productClient.getProductById(501L)).thenReturn(ProductSummaryResponse.builder()
                .productId(501L)
                .sku("SKU-501")
                .name("Industrial Drill")
                .costPrice(new BigDecimal("120.50"))
                .isActive(Boolean.TRUE)
                .build());
        when(purchaseRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder saved = invocation.getArgument(0);
            saved.setPoId(9002L);
            return saved;
        });

        PurchaseOrder saved = purchaseService.createPO(request);

        assertThat(saved.getPoId()).isEqualTo(9002L);
        verify(alertEventPublisher, never()).publishPoPendingApprovalEvent(any(PoPendingApprovalEvent.class));
    }

    private PurchaseOrder purchaseOrder(PurchaseOrderStatus status) {
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .supplierId(7L)
                .warehouseId(3L)
                .createdById(55L)
                .status(status)
                .orderDate(LocalDate.of(2026, 4, 25))
                .expectedDate(LocalDate.of(2026, 4, 30))
                .referenceNumber("PO-REF-1001")
                .lineItems(List.of(POLineItem.builder()
                        .productId(501L)
                        .quantity(10)
                        .unitCost(null)
                        .receivedQty(0)
                        .build()))
                .build();
        return purchaseOrder;
    }
}

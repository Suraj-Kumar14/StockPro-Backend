package com.stockpro.purchase.controller;

import com.stockpro.purchase.entity.POLineItem;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@Tag(name = "Purchase Order Management", description = "Procurement lifecycle APIs for StockPro purchase orders.")
@SecurityRequirement(name = "bearerAuth")
public class PurchaseResource {

    private final PurchaseService purchaseService;

    public PurchaseResource(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PURCHASE_OFFICER')")
    @Operation(summary = "Create a new purchase order")
    public ResponseEntity<PurchaseOrder> createPO(@RequestBody PurchaseOrder purchaseOrder) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.createPO(purchaseOrder));
    }

    @PutMapping("/{poId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER')")
    @Operation(summary = "Approve a purchase order")
    public ResponseEntity<PurchaseOrder> approvePO(@PathVariable Long poId) {
        return ResponseEntity.ok(purchaseService.approvePO(poId));
    }

    @PostMapping("/{poId}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Receive goods for a purchase order and update warehouse stock")
    public ResponseEntity<PurchaseOrder> receiveGoods(@PathVariable Long poId,
            @RequestBody List<POLineItem> receivedItems) {
        return ResponseEntity.ok(purchaseService.receiveGoods(poId, receivedItems));
    }

    @PutMapping("/{poId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PURCHASE_OFFICER')")
    @Operation(summary = "Cancel a purchase order before goods are received")
    public ResponseEntity<PurchaseOrder> cancelPO(@PathVariable Long poId) {
        return ResponseEntity.ok(purchaseService.cancelPO(poId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'PURCHASE_OFFICER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get purchase orders by status")
    public ResponseEntity<List<PurchaseOrder>> getPOsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(purchaseService.getPOsByStatus(status));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER', 'MANAGER', 'PURCHASE_OFFICER', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get purchase orders within an order-date range")
    public ResponseEntity<List<PurchaseOrder>> getPOsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(purchaseService.getPOsByDateRange(start, end));
    }
}

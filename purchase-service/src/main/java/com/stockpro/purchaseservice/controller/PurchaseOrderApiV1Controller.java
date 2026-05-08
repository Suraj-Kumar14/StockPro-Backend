package com.stockpro.purchaseservice.controller;

import com.stockpro.purchaseservice.dto.request.*;
import com.stockpro.purchaseservice.dto.response.*;
import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.security.AuthenticatedUser;
import com.stockpro.purchaseservice.service.PurchaseOrderManagementService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderApiV1Controller {

    private final PurchaseOrderManagementService purchaseOrderManagementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OFFICER','ADMIN')")
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderManagementService.createPurchaseOrder(request, actorId(authentication)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OFFICER','ADMIN')")
    public PurchaseOrderResponse update(@PathVariable Long id, @Valid @RequestBody UpdatePurchaseOrderRequest request, Authentication authentication) {
        return purchaseOrderManagementService.updatePurchaseOrder(id, request, actorId(authentication));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER','STAFF')")
    public Page<PurchaseOrderResponse> getAll(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "desc") String sortDir) {
        return purchaseOrderManagementService.getAllPurchaseOrders(page, size, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER','STAFF')")
    public Page<PurchaseOrderResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) POStatus status,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Boolean overdueOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return purchaseOrderManagementService.searchPurchaseOrders(keyword, supplierId, warehouseId, status, createdBy, fromDate, toDate, overdueOnly, page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER','STAFF')")
    public PurchaseOrderResponse getById(@PathVariable Long id) {
        return purchaseOrderManagementService.getPurchaseOrderById(id);
    }

    @GetMapping("/number/{poNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER','STAFF')")
    public PurchaseOrderResponse getByNumber(@PathVariable String poNumber) {
        return purchaseOrderManagementService.getPurchaseOrderByNumber(poNumber);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER','STAFF')")
    public List<PurchaseOrderResponse> getByStatus(@PathVariable POStatus status) {
        return purchaseOrderManagementService.getPurchaseOrdersByStatus(status);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('OFFICER','ADMIN')")
    public PurchaseOrderResponse submit(@PathVariable Long id, @RequestBody(required = false) SubmitPurchaseOrderRequest request, Authentication authentication) {
        return purchaseOrderManagementService.submitPurchaseOrder(id, request, actorId(authentication));
    }

    @PostMapping("/{id}/submit-for-payment")
    @PreAuthorize("hasAnyRole('OFFICER','ADMIN')")
    public PurchaseOrderResponse submitForPayment(@PathVariable Long id, Authentication authentication) {
        return purchaseOrderManagementService.submitForPayment(id, actorId(authentication));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public PurchaseOrderResponse approve(@PathVariable Long id, @RequestBody(required = false) ApprovePurchaseOrderRequest request, Authentication authentication) {
        return purchaseOrderManagementService.approvePurchaseOrder(id, request, actorId(authentication));
    }

    @PostMapping("/{id}/payment-initiated")
    @PreAuthorize("hasAnyRole('OFFICER','ADMIN','MANAGER')")
    public PurchaseOrderResponse markPaymentInitiated(@PathVariable Long id, @Valid @RequestBody PaymentTransitionRequest request) {
        return purchaseOrderManagementService.markPaymentInitiated(id, request);
    }

    @PostMapping("/{id}/payment-completed")
    @PreAuthorize("hasAnyRole('OFFICER','ADMIN','MANAGER')")
    public PurchaseOrderResponse markPaymentCompleted(@PathVariable Long id, @Valid @RequestBody PaymentTransitionRequest request) {
        return purchaseOrderManagementService.markPaymentCompleted(id, request);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public PurchaseOrderResponse reject(@PathVariable Long id, @Valid @RequestBody RejectPurchaseOrderRequest request, Authentication authentication) {
        return purchaseOrderManagementService.rejectPurchaseOrder(id, request, actorId(authentication));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OFFICER','MANAGER','ADMIN')")
    public PurchaseOrderResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelPurchaseOrderRequest request, Authentication authentication) {
        return purchaseOrderManagementService.cancelPurchaseOrder(id, request, actorId(authentication));
    }

    @PostMapping("/{poId}/receive")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public PurchaseOrderResponse receivePurchaseOrder(@PathVariable Long poId, @Valid @RequestBody ReceivePurchaseOrderRequest request, Authentication authentication) {
        return purchaseOrderManagementService.receivePurchaseOrder(poId, request, actorId(authentication));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER','STAFF')")
    public List<PurchaseOrderHistoryResponse> history(@PathVariable Long id) {
        return purchaseOrderManagementService.getPurchaseOrderHistory(id);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER')")
    public PurchaseOrderSummaryResponse summary() {
        return purchaseOrderManagementService.getPurchaseOrderSummary();
    }

    @GetMapping("/purchase-officer/summary")
    @PreAuthorize("hasAnyRole('OFFICER','ADMIN')")
    public PurchaseOrderSummaryResponse purchaseOfficerSummary(Authentication authentication) {
        return purchaseOrderManagementService.getPurchaseOfficerSummary(actorId(authentication));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER')")
    public Page<PurchaseOrderReportRowResponse> reports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) POStatus status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return purchaseOrderManagementService.getPurchaseOrderReports(keyword, status, paymentStatus, supplierId, fromDate, toDate, page, size);
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER')")
    public PurchaseAnalyticsResponse analytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return purchaseOrderManagementService.getPurchaseAnalytics(fromDate, toDate);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OFFICER')")
    public List<PurchaseOrderResponse> overdue() {
        return purchaseOrderManagementService.getOverduePurchaseOrders();
    }

    @GetMapping("/pending-approval")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<PurchaseOrderResponse> pendingApproval() {
        return purchaseOrderManagementService.getPendingApprovalPurchaseOrders();
    }

    private Long actorId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }
}

package com.stockpro.paymentservice.controller;

import com.stockpro.paymentservice.dto.request.ApprovePaymentRequest;
import com.stockpro.paymentservice.dto.request.CancelPaymentRequest;
import com.stockpro.paymentservice.dto.request.CreatePaymentRequest;
import com.stockpro.paymentservice.dto.request.MarkPaymentPaidRequest;
import com.stockpro.paymentservice.dto.request.PaymentSearchRequest;
import com.stockpro.paymentservice.dto.request.RejectPaymentRequest;
import com.stockpro.paymentservice.dto.request.ReversePaymentRequest;
import com.stockpro.paymentservice.dto.request.SubmitPaymentRequest;
import com.stockpro.paymentservice.dto.request.UpdatePaymentRequest;
import com.stockpro.paymentservice.dto.response.PaymentAnalyticsResponse;
import com.stockpro.paymentservice.dto.response.PaymentHistoryResponse;
import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.PaymentSummaryResponse;
import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.security.AuthenticatedUser;
import com.stockpro.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Management", description = "APIs for supplier payment management and lifecycle operations")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @Operation(summary = "Create a payment draft")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request, Authentication authentication) {
        log.info("Creating payment draft for purchaseOrderId={}", request.purchaseOrderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request, actorId(authentication)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @Operation(summary = "Update a draft or pending payment")
    public PaymentResponse updatePayment(@PathVariable Long id, @Valid @RequestBody UpdatePaymentRequest request, Authentication authentication) {
        return paymentService.updatePayment(id, request, actorId(authentication));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get all payments")
    public Page<PaymentResponse> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return paymentService.getAllPayments(page, size, sortBy, sortDir);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Search payments with filters")
    public Page<PaymentResponse> searchPayments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long purchaseOrderId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return paymentService.searchPayments(new PaymentSearchRequest(
                keyword, purchaseOrderId, supplierId, status, paymentMethod, createdBy,
                fromDate, toDate, minAmount, maxAmount, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER','STAFF')")
    @Operation(summary = "Get payment by ID")
    public PaymentResponse getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }

    @GetMapping("/number/{paymentNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get payment by payment number")
    public PaymentResponse getPaymentByNumber(@PathVariable String paymentNumber) {
        return paymentService.getPaymentByNumber(paymentNumber);
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER','STAFF')")
    @Operation(summary = "Get payments by purchase order")
    public Page<PaymentResponse> getPaymentsByPurchaseOrder(
            @PathVariable Long purchaseOrderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return paymentService.getPaymentsByPurchaseOrder(purchaseOrderId, page, size);
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get payments by supplier")
    public Page<PaymentResponse> getPaymentsBySupplier(
            @PathVariable Long supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return paymentService.getPaymentsBySupplier(supplierId, page, size);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @Operation(summary = "Submit a payment for approval")
    public PaymentResponse submitPayment(@PathVariable Long id, @RequestBody(required = false) SubmitPaymentRequest request, Authentication authentication) {
        return paymentService.submitPayment(id, request, actorId(authentication));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Approve a pending payment")
    public PaymentResponse approvePayment(@PathVariable Long id, @RequestBody(required = false) ApprovePaymentRequest request, Authentication authentication) {
        return paymentService.approvePayment(id, request, actorId(authentication));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Reject a pending payment")
    public PaymentResponse rejectPayment(@PathVariable Long id, @Valid @RequestBody RejectPaymentRequest request, Authentication authentication) {
        return paymentService.rejectPayment(id, request, actorId(authentication));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @Operation(summary = "Cancel a draft, pending, or approved payment")
    public PaymentResponse cancelPayment(@PathVariable Long id, @Valid @RequestBody CancelPaymentRequest request, Authentication authentication) {
        return paymentService.cancelPayment(id, request, actorId(authentication));
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @Operation(summary = "Mark an approved payment as paid or partially paid")
    public PaymentResponse markPaymentPaid(@PathVariable Long id, @Valid @RequestBody MarkPaymentPaidRequest request, Authentication authentication) {
        return paymentService.markPaymentPaid(id, request, actorId(authentication));
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reverse a paid payment")
    public PaymentResponse reversePayment(@PathVariable Long id, @Valid @RequestBody ReversePaymentRequest request, Authentication authentication) {
        return paymentService.reversePayment(id, request, actorId(authentication));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get payment history timeline")
    public List<PaymentHistoryResponse> getPaymentHistory(@PathVariable Long id) {
        return paymentService.getPaymentHistory(id);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get payment summary")
    public PaymentSummaryResponse getPaymentSummary() {
        return paymentService.getPaymentSummary();
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get payment analytics")
    public PaymentAnalyticsResponse getPaymentAnalytics(
            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return paymentService.getPaymentAnalytics(fromDate, toDate);
    }

    @GetMapping("/purchase-order/{purchaseOrderId}/paid-amount")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get total paid amount for a purchase order")
    public BigDecimal getPaidAmountForPurchaseOrder(@PathVariable Long purchaseOrderId) {
        return paymentService.getPaidAmountForPurchaseOrder(purchaseOrderId);
    }

    @GetMapping("/purchase-order/{purchaseOrderId}/remaining-amount")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get remaining payable amount for a purchase order")
    public BigDecimal getRemainingAmountForPurchaseOrder(@PathVariable Long purchaseOrderId) {
        return paymentService.getRemainingAmountForPurchaseOrder(purchaseOrderId);
    }

    private Long actorId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }
}

package com.stockpro.paymentservice.controller;

import com.stockpro.paymentservice.dto.request.RazorpayInitiateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayPaymentStatusUpdateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayVerifyRequest;
import com.stockpro.paymentservice.dto.request.SplitPaymentPlanRequest;
import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.PaymentSummaryResponse;
import com.stockpro.paymentservice.dto.response.RazorpayOrderResponse;
import com.stockpro.paymentservice.dto.response.RemainingAmountResponse;
import com.stockpro.paymentservice.dto.response.SplitPaymentPlanResponse;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.security.AuthenticatedUser;
import com.stockpro.paymentservice.service.PaymentService;
import com.stockpro.paymentservice.service.RazorpayPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Razorpay payment APIs for purchase orders")
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayPaymentService razorpayPaymentService;

    // ─── Static Routes (must be declared before dynamic /{paymentId} routes) ──

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "List all payments (paginated)")
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.debug("GET /api/v1/payments page={} size={} sortBy={} sortDir={}", page, size, sortBy, sortDir);
        return ResponseEntity.ok(paymentService.getAllPayments(page, size, sortBy, sortDir));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Search payments with enterprise filters")
    public ResponseEntity<Page<PaymentResponse>> searchPayments(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("[PaymentController] GET /search supplierId={} status={} fromDate={} toDate={} page={} size={}",
                supplierId, status, fromDate, toDate, page, size);
        return ResponseEntity.ok(paymentService.searchPayments(supplierId, status, fromDate, toDate, page, size, sortBy, sortDir));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get payment summary")
    public ResponseEntity<PaymentSummaryResponse> getPaymentSummary() {
        log.info("[PaymentController] GET /summary");
        return ResponseEntity.ok(paymentService.getPaymentSummary());
    }

    @GetMapping("/purchase-order/{purchaseOrderId}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER','STAFF')")
    @Operation(summary = "Get payments for a purchase order")
    public ResponseEntity<Page<PaymentResponse>> getPaymentsByPurchaseOrder(
            @PathVariable Long purchaseOrderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("GET /api/v1/payments/purchase-order/{} page={} size={}", purchaseOrderId, page, size);
        return ResponseEntity.ok(paymentService.getPaymentsByPurchaseOrder(purchaseOrderId, page, size));
    }

    @GetMapping("/purchase-order/{purchaseOrderId}/paid-amount")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get total paid amount for a purchase order")
    public ResponseEntity<BigDecimal> getPaidAmount(@PathVariable Long purchaseOrderId) {
        log.debug("GET /api/v1/payments/purchase-order/{}/paid-amount", purchaseOrderId);
        return ResponseEntity.ok(paymentService.getPaidAmountForPurchaseOrder(purchaseOrderId));
    }

    @GetMapping("/purchase-order/{purchaseOrderId}/remaining-amount")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Get remaining payable amount for a purchase order")
    public ResponseEntity<RemainingAmountResponse> getRemainingAmount(
            @PathVariable Long purchaseOrderId,
            HttpServletRequest request) {
        log.debug("GET /api/v1/payments/purchase-order/{}/remaining-amount", purchaseOrderId);
        return ResponseEntity.ok(
                razorpayPaymentService.getRemainingAmount(purchaseOrderId, request.getHeader("Authorization")));
    }

    // ─── Dynamic Route — numeric IDs only (regex \d+ prevents capturing static paths) ─

    @GetMapping("/{paymentId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER','STAFF')")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long paymentId) {
        log.debug("GET /api/v1/payments/{}", paymentId);
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    // ─── Razorpay Endpoints ───────────────────────────────────────────────────

    @PostMapping("/razorpay/initiate")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Initiate Razorpay payment for an approved Purchase Order")
    public ResponseEntity<RazorpayOrderResponse> initiateRazorpayPayment(
            @Valid @RequestBody RazorpayInitiateRequest razorpayRequest,
            Authentication authentication,
            HttpServletRequest request) {
        log.info("[PaymentController] POST /razorpay/initiate purchaseOrderId={} actorId={} actorRole={} amount={} action={}",
                razorpayRequest.purchaseOrderId(), actorId(authentication), actorRole(authentication), razorpayRequest.paymentAmount(),
                razorpayRequest.paymentAmount() != null ? "SPLIT_OR_REMAINING" : "FULL_PAYMENT");
        RazorpayOrderResponse response = razorpayPaymentService.initiatePayment(
                razorpayRequest,
                actorId(authentication),
                request.getHeader("Authorization"),
                canSplitPayments(authentication));
        log.info("[PaymentController] Razorpay order created razorpayOrderId={} purchaseOrderId={}",
                response.getRazorpayOrderId(), razorpayRequest.purchaseOrderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/split-plan")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate a split-payment plan for amounts above the Razorpay transaction limit")
    public ResponseEntity<SplitPaymentPlanResponse> generateSplitPlan(
            @Valid @RequestBody SplitPaymentPlanRequest splitRequest,
            Authentication authentication,
            HttpServletRequest request) {
        log.info("[PaymentController] POST /split-plan purchaseOrderId={} requestedAmount={} actorId={} actorRole={} action=SPLIT_PLAN",
                splitRequest.purchaseOrderId(), splitRequest.requestedAmount(), actorId(authentication), actorRole(authentication));
        return ResponseEntity.ok(razorpayPaymentService.getSplitPaymentPlan(
                splitRequest,
                request.getHeader("Authorization")));
    }

    @PostMapping("/razorpay/verify")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Verify completed Razorpay payment and mark as PAID")
    public ResponseEntity<PaymentResponse> verifyRazorpayPayment(
            @Valid @RequestBody RazorpayVerifyRequest verifyRequest,
            Authentication authentication,
            HttpServletRequest request) {
        log.info("[PaymentController] POST /razorpay/verify razorpayOrderId={} actorId={} actorRole={} action=VERIFY",
                verifyRequest.razorpayOrderId(), actorId(authentication), actorRole(authentication));
        return ResponseEntity.ok(
                razorpayPaymentService.verifyPayment(verifyRequest, actorId(authentication), request.getHeader("Authorization")));
    }

    @PostMapping("/razorpay/failure")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Record a failed Razorpay checkout without marking the purchase order as paid")
    public ResponseEntity<PaymentResponse> recordRazorpayFailure(
            @Valid @RequestBody RazorpayPaymentStatusUpdateRequest failureRequest,
            Authentication authentication) {
        log.info("[PaymentController] POST /razorpay/failure razorpayOrderId={} actorId={}",
                failureRequest.razorpayOrderId(), actorId(authentication));
        return ResponseEntity.ok(razorpayPaymentService.recordFailedPayment(failureRequest, actorId(authentication)));
    }

    @PostMapping("/razorpay/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','MANAGER')")
    @Operation(summary = "Record a cancelled Razorpay checkout without marking the purchase order as paid")
    public ResponseEntity<PaymentResponse> recordRazorpayCancellation(
            @Valid @RequestBody RazorpayPaymentStatusUpdateRequest cancellationRequest,
            Authentication authentication) {
        log.info("[PaymentController] POST /razorpay/cancel razorpayOrderId={} actorId={}",
                cancellationRequest.razorpayOrderId(), actorId(authentication));
        return ResponseEntity.ok(razorpayPaymentService.recordCancelledPayment(cancellationRequest, actorId(authentication)));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Long actorId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.userId();
        }
        return null;
    }

    private boolean canSplitPayments(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return "ADMIN".equalsIgnoreCase(user.role());
        }
        return false;
    }

    private String actorRole(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.role();
        }
        return "UNKNOWN";
    }
}

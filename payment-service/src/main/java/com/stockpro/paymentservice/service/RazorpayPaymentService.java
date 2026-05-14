package com.stockpro.paymentservice.service;

import com.stockpro.paymentservice.dto.request.RazorpayInitiateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayPaymentStatusUpdateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayVerifyRequest;
import com.stockpro.paymentservice.dto.request.SplitPaymentPlanRequest;
import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.RazorpayOrderResponse;
import com.stockpro.paymentservice.dto.response.RemainingAmountResponse;
import com.stockpro.paymentservice.dto.response.SplitPaymentPlanResponse;

public interface RazorpayPaymentService {

    /**
     * Creates a Razorpay order for the given purchase order and saves a PENDING payment record.
     *
     * @param request   contains purchaseOrderId
     * @param actorId   the authenticated user's ID
     * @param authToken the Bearer JWT token to forward to purchase-service
     * @return RazorpayOrderResponse with razorpayOrderId, amount, keyId for frontend checkout
     */
    RazorpayOrderResponse initiatePayment(RazorpayInitiateRequest request, Long actorId, String authToken, boolean splitPaymentAllowed);

    /**
     * Verifies the Razorpay signature and marks the payment as PAID.
     *
     * @param request contains razorpayOrderId, razorpayPaymentId, razorpaySignature
     * @param actorId the authenticated user's ID
     * @return PaymentResponse with updated PAID status
     */
    PaymentResponse verifyPayment(RazorpayVerifyRequest request, Long actorId, String authToken);

    /**
     * Records a Razorpay checkout failure without marking the purchase order as paid.
     */
    PaymentResponse recordFailedPayment(RazorpayPaymentStatusUpdateRequest request, Long actorId);

    /**
     * Records a cancelled Razorpay checkout without marking the purchase order as paid.
     */
    PaymentResponse recordCancelledPayment(RazorpayPaymentStatusUpdateRequest request, Long actorId);

    /**
     * Returns the remaining payable amount for a purchase order.
     */
    RemainingAmountResponse getRemainingAmount(Long purchaseOrderId, String authToken);

    /**
     * Returns a backend-generated split plan for amounts exceeding the Razorpay transaction limit.
     */
    SplitPaymentPlanResponse getSplitPaymentPlan(SplitPaymentPlanRequest request, String authToken);
}

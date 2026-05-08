package com.stockpro.paymentservice.service;

import com.stockpro.paymentservice.dto.request.RazorpayInitiateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayVerifyRequest;
import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.RazorpayOrderResponse;
import com.stockpro.paymentservice.dto.response.RemainingAmountResponse;

public interface RazorpayPaymentService {

    /**
     * Creates a Razorpay order for the given purchase order and saves a PENDING payment record.
     *
     * @param request   contains purchaseOrderId
     * @param actorId   the authenticated user's ID
     * @param authToken the Bearer JWT token to forward to purchase-service
     * @return RazorpayOrderResponse with razorpayOrderId, amount, keyId for frontend checkout
     */
    RazorpayOrderResponse initiatePayment(RazorpayInitiateRequest request, Long actorId, String authToken);

    /**
     * Verifies the Razorpay signature and marks the payment as PAID.
     *
     * @param request contains razorpayOrderId, razorpayPaymentId, razorpaySignature
     * @param actorId the authenticated user's ID
     * @return PaymentResponse with updated PAID status
     */
    PaymentResponse verifyPayment(RazorpayVerifyRequest request, Long actorId, String authToken);

    /**
     * Returns the remaining payable amount for a purchase order.
     */
    RemainingAmountResponse getRemainingAmount(Long purchaseOrderId, String authToken);
}

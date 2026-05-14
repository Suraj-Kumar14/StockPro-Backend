package com.stockpro.paymentservice.exception;

import java.math.BigDecimal;

public class PaymentLimitExceededException extends RuntimeException {

    private final BigDecimal requestedAmount;
    private final BigDecimal maxAllowedAmount;
    private final BigDecimal remainingAmount;
    private final boolean splitAllowed;

    public PaymentLimitExceededException(String message,
                                         BigDecimal requestedAmount,
                                         BigDecimal maxAllowedAmount,
                                         BigDecimal remainingAmount,
                                         boolean splitAllowed) {
        super(message);
        this.requestedAmount = requestedAmount;
        this.maxAllowedAmount = maxAllowedAmount;
        this.remainingAmount = remainingAmount;
        this.splitAllowed = splitAllowed;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public BigDecimal getMaxAllowedAmount() {
        return maxAllowedAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public boolean isSplitAllowed() {
        return splitAllowed;
    }
}

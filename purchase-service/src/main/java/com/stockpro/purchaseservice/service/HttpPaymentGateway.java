package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.PaymentStatusSnapshotDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpPaymentGateway implements PaymentGateway {
    private final RestClient.Builder restClientBuilder;
    private final DownstreamAuthSupport downstreamAuthSupport;

    @Value("${payment-service.base-url:http://localhost:8080/api/v1/payments}")
    private String paymentServiceBaseUrl;

    @Override
    public PaymentStatusSnapshotDTO getPaymentStatusSnapshot(Long purchaseOrderId) {
        try {
            PaymentPageResponse response = restClientBuilder.baseUrl(paymentServiceBaseUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/purchase-order/{purchaseOrderId}")
                            .queryParam("page", 0)
                            .queryParam("size", 20)
                            .build(purchaseOrderId))
                    .headers(downstreamAuthSupport::apply)
                    .retrieve()
                    .body(PaymentPageResponse.class);

            List<PaymentSummaryItem> payments = response != null && response.getContent() != null
                    ? response.getContent()
                    : List.of();

            PaymentSummaryItem latest = payments.stream()
                    .findFirst()
                    .orElse(null);
            PaymentSummaryItem fullyPaid = payments.stream()
                    .filter(payment -> "PAID".equalsIgnoreCase(payment.getStatus()))
                    .findFirst()
                    .orElse(null);
            PaymentSummaryItem partiallyPaid = payments.stream()
                    .filter(payment -> "PARTIALLY_PAID".equalsIgnoreCase(payment.getStatus()))
                    .findFirst()
                    .orElse(null);
            PaymentSummaryItem selected = selectPreferredPayment(latest, fullyPaid, partiallyPaid);

            return PaymentStatusSnapshotDTO.builder()
                    .paymentId(selected != null ? selected.getPaymentId() : null)
                    .paymentNumber(selected != null ? selected.getPaymentNumber() : null)
                    .paymentStatus(selected != null ? selected.getStatus() : "UNPAID")
                    .paymentCompleted(fullyPaid != null)
                    .paymentAmount(selected != null ? selected.getPaymentAmount() : null)
                    .razorpayOrderId(selected != null ? selected.getRazorpayOrderId() : null)
                    .razorpayPaymentId(selected != null ? selected.getRazorpayPaymentId() : null)
                    .paidAt(selected != null ? selected.getPaidAt() : null)
                    .build();
        } catch (RestClientResponseException ex) {
            log.warn("Payment lookup failed for purchaseOrderId={} status={} body={}",
                    purchaseOrderId, ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (RestClientException ex) {
            log.warn("Payment-service unavailable for purchaseOrderId={}: {}", purchaseOrderId, ex.getMessage());
        }

        return PaymentStatusSnapshotDTO.builder()
                .paymentStatus("UNPAID")
                .paymentCompleted(false)
                .build();
    }

    private PaymentSummaryItem selectPreferredPayment(
            PaymentSummaryItem latest,
            PaymentSummaryItem fullyPaid,
            PaymentSummaryItem partiallyPaid) {
        if (fullyPaid != null) {
            return fullyPaid;
        }
        if (partiallyPaid != null) {
            return partiallyPaid;
        }
        return latest;
    }

    @Data
    private static class PaymentPageResponse {
        private List<PaymentSummaryItem> content;
    }

    @Data
    private static class PaymentSummaryItem {
        private Long paymentId;
        private String paymentNumber;
        private String status;
        private BigDecimal paymentAmount;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private LocalDateTime paidAt;
    }
}

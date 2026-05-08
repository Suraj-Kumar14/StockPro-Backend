package com.stockpro.paymentservice.client;

import com.stockpro.paymentservice.exception.ExternalServiceException;
import com.stockpro.paymentservice.exception.PaymentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpPurchaseServiceClient implements PurchaseServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${purchase-service.base-url:http://localhost:8080/api/v1/purchase-orders}")
    private String purchaseServiceBaseUrl;

    /**
     * No-auth version — used by the existing manual payment workflow.
     * Attempts the call without Authorization header.
     */
    @Override
    public PurchaseOrderLookupResponse getPurchaseOrder(Long purchaseOrderId) {
        return getPurchaseOrder(purchaseOrderId, null);
    }

    /**
     * Auth-aware version — passes the caller's JWT so the API Gateway / purchase-service
     * can authenticate the service-to-service request.
     */
    @Override
    public PurchaseOrderLookupResponse getPurchaseOrder(Long purchaseOrderId, String authToken) {
        try {
            RestClient.RequestHeadersSpec<?> spec = restClientBuilder
                    .baseUrl(purchaseServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/{id}", purchaseOrderId);

            if (authToken != null && !authToken.isBlank()) {
                spec = spec.header("Authorization", authToken.startsWith("Bearer ") ? authToken : "Bearer " + authToken);
            }

            PurchaseOrderLookupResponse response = spec
                    .retrieve()
                    .body(PurchaseOrderLookupResponse.class);

            if (response == null || (response.getPoId() == null && response.getPurchaseOrderId() == null)) {
                throw new PaymentValidationException("Purchase order not found: ID " + purchaseOrderId);
            }
            if (response.getPurchaseOrderId() == null) {
                response.setPurchaseOrderId(response.getPoId());
            }
            return response;

        } catch (PaymentValidationException e) {
            throw e;
        } catch (RestClientResponseException ex) {
            HttpStatusCode code = ex.getStatusCode();
            log.warn("Purchase-service returned {} for purchaseOrderId={}", code, purchaseOrderId);
            if (code.is4xxClientError()) {
                throw new PaymentValidationException("Purchase order not found or inaccessible: ID " + purchaseOrderId);
            }
            throw new ExternalServiceException("Purchase-service error: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            log.error("Purchase-service lookup failed for purchaseOrderId={}: {}", purchaseOrderId, ex.getMessage());
            throw new ExternalServiceException("Purchase-service unavailable. Please try again later.", ex);
        }
    }

    @Override
    public void markPaymentInitiated(Long purchaseOrderId, PaymentTransitionRequest request, String authToken) {
        postPaymentTransition(purchaseOrderId, "/{id}/payment-initiated", request, authToken);
    }

    @Override
    public void markPaymentCompleted(Long purchaseOrderId, PaymentTransitionRequest request, String authToken) {
        postPaymentTransition(purchaseOrderId, "/{id}/payment-completed", request, authToken);
    }

    private void postPaymentTransition(Long purchaseOrderId, String path, PaymentTransitionRequest request, String authToken) {
        try {
            RestClient.RequestBodySpec spec = restClientBuilder
                    .baseUrl(purchaseServiceBaseUrl)
                    .build()
                    .post()
                    .uri(path, purchaseOrderId);

            if (authToken != null && !authToken.isBlank()) {
                spec = spec.header("Authorization", authToken.startsWith("Bearer ") ? authToken : "Bearer " + authToken);
            }

            spec.body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Purchase-service payment transition failed for purchaseOrderId={} status={} body={}",
                    purchaseOrderId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ExternalServiceException("Purchase-service payment status update failed", ex);
        } catch (RestClientException ex) {
            log.error("Purchase-service payment transition transport failure for purchaseOrderId={}: {}", purchaseOrderId, ex.getMessage());
            throw new ExternalServiceException("Purchase-service unavailable. Please try again later.", ex);
        }
    }
}

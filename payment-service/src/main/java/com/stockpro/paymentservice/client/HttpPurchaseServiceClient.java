package com.stockpro.paymentservice.client;

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

    @Override
    public PurchaseOrderLookupResponse getPurchaseOrder(Long purchaseOrderId) {
        try {
            PurchaseOrderLookupResponse response = restClientBuilder.baseUrl(purchaseServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/{purchaseOrderId}", purchaseOrderId)
                    .retrieve()
                    .body(PurchaseOrderLookupResponse.class);
            if (response == null || (response.getPoId() == null && response.getPurchaseOrderId() == null)) {
                throw new PaymentValidationException("Purchase order not found");
            }
            if (response.getPurchaseOrderId() == null) {
                response.setPurchaseOrderId(response.getPoId());
            }
            return response;
        } catch (RestClientResponseException ex) {
            HttpStatusCode code = ex.getStatusCode();
            if (code.is4xxClientError()) {
                throw new PaymentValidationException("Purchase order not found");
            }
            throw new IllegalStateException("Purchase-service unavailable", ex);
        } catch (RestClientException ex) {
            log.error("Purchase-service lookup failed for purchaseOrderId={}: {}", purchaseOrderId, ex.getMessage());
            throw new IllegalStateException("Purchase-service unavailable", ex);
        }
    }
}

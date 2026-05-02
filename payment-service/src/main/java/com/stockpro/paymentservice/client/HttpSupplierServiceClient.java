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
public class HttpSupplierServiceClient implements SupplierServiceClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${supplier-service.base-url:http://localhost:8080/api/v1/suppliers}")
    private String supplierServiceBaseUrl;

    @Override
    public SupplierLookupResponse getSupplier(Long supplierId) {
        try {
            SupplierLookupResponse response = restClientBuilder.baseUrl(supplierServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/{supplierId}", supplierId)
                    .retrieve()
                    .body(SupplierLookupResponse.class);
            if (response == null || response.getSupplierId() == null) {
                throw new PaymentValidationException("Supplier not found or inactive");
            }
            if (Boolean.FALSE.equals(response.getIsActive()) || "BLACKLISTED".equalsIgnoreCase(response.getStatus())
                    || "INACTIVE".equalsIgnoreCase(response.getStatus())) {
                throw new PaymentValidationException("Supplier not found or inactive");
            }
            return response;
        } catch (RestClientResponseException ex) {
            HttpStatusCode code = ex.getStatusCode();
            if (code.is4xxClientError()) {
                throw new PaymentValidationException("Supplier not found or inactive");
            }
            throw new IllegalStateException("Supplier-service unavailable", ex);
        } catch (RestClientException ex) {
            log.error("Supplier-service lookup failed for supplierId={}: {}", supplierId, ex.getMessage());
            throw new IllegalStateException("Supplier-service unavailable", ex);
        }
    }
}

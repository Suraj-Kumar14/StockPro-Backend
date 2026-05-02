package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.SupplierLookupResponseDTO;
import com.stockpro.purchaseservice.exception.SupplierNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
@RequiredArgsConstructor
public class HttpSupplierGateway implements SupplierGateway {

    private final RestClient.Builder restClientBuilder;

    @Value("${supplier-service.base-url:http://localhost:8080/api/v1/suppliers}")
    private String supplierServiceBaseUrl;

    @Override
    public void ensureSupplierExists(Long supplierId) {
        getSupplier(supplierId);
    }

    @Override
    public SupplierLookupResponseDTO getSupplier(Long supplierId) {
        try {
            SupplierLookupResponseDTO response = restClientBuilder.baseUrl(supplierServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/{supplierId}", supplierId)
                    .retrieve()
                    .body(SupplierLookupResponseDTO.class);

            if (response == null || response.getSupplierId() == null) {
                throw new SupplierNotFoundException(
                        "Supplier not found with ID: " + supplierId);
            }
            if (Boolean.FALSE.equals(response.getIsActive())) {
                throw new SupplierNotFoundException(
                        "Supplier is inactive with ID: " + supplierId);
            }
            return response;
        } catch (RestClientResponseException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            if (statusCode.is4xxClientError()) {
                throw new SupplierNotFoundException(
                        "Supplier not found with ID: " + supplierId);
            }
            throw new IllegalStateException("Supplier-service unavailable", ex);
        } catch (RestClientException ex) {
            log.error("Supplier lookup failed for supplier {}: {}", supplierId, ex.getMessage());
            throw new IllegalStateException("Supplier-service unavailable", ex);
        }
    }
}

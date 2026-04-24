package com.stockpro.purchase.client;

import com.stockpro.purchase.config.FeignAuthForwardingConfig;
import com.stockpro.purchase.dto.SupplierSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "${supplier.service.name:SUPPLIER-SERVICE}",
        configuration = FeignAuthForwardingConfig.class,
        path = "${supplier.service.path:/api/v1/suppliers}")
public interface SupplierClient {

    @GetMapping("/{supplierId}")
    SupplierSummaryResponse getSupplierById(@PathVariable Long supplierId);
}

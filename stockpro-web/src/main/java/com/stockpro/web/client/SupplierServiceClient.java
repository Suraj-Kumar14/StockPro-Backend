package com.stockpro.web.client;

import com.stockpro.web.config.FeignSupportConfig;
import com.stockpro.web.dto.request.SupplierFormRequest;
import com.stockpro.web.dto.response.SupplierResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "supplierServiceClient",
        url = "${api.gateway.base-url}",
        configuration = FeignSupportConfig.class)
public interface SupplierServiceClient {

    @GetMapping("/api/v1/suppliers/all")
    List<SupplierResponse> getAllSuppliers();

    @GetMapping("/api/v1/suppliers/{supplierId}")
    SupplierResponse getSupplierById(@PathVariable("supplierId") Long supplierId);

    @GetMapping("/api/v1/suppliers/search")
    List<SupplierResponse> searchSuppliers(@RequestParam(required = false) String name);

    @GetMapping("/api/v1/suppliers/city/{city}")
    List<SupplierResponse> getSuppliersByCity(@PathVariable("city") String city);

    @GetMapping("/api/v1/suppliers/country/{country}")
    List<SupplierResponse> getSuppliersByCountry(@PathVariable("country") String country);

    @PostMapping("/api/v1/suppliers")
    SupplierResponse createSupplier(@RequestBody SupplierFormRequest request);

    @PutMapping("/api/v1/suppliers/{supplierId}")
    SupplierResponse updateSupplier(@PathVariable("supplierId") Long supplierId,
            @RequestBody SupplierFormRequest request);

    @PutMapping("/api/v1/suppliers/{supplierId}/deactivate")
    SupplierResponse deactivateSupplier(@PathVariable("supplierId") Long supplierId);
}

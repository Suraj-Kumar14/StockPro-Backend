package com.stockpro.supplierservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlacklistSupplierRequest(
        @NotBlank @Size(max = 500) String reason
) {}

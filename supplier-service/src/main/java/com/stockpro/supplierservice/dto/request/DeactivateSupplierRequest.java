package com.stockpro.supplierservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeactivateSupplierRequest(
        @NotBlank @Size(max = 500) String reason
) {}

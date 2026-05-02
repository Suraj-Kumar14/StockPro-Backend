package com.stockpro.supplierservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateSupplierRatingRequest(
        @DecimalMin(value = "0.0") @DecimalMax(value = "5.0") BigDecimal rating,
        @DecimalMin(value = "0.0") @DecimalMax(value = "5.0") BigDecimal qualityRating,
        @DecimalMin(value = "0.0") @DecimalMax(value = "5.0") BigDecimal deliveryRating,
        @DecimalMin(value = "0.0") @DecimalMax(value = "5.0") BigDecimal communicationRating,
        @DecimalMin(value = "0.0") @DecimalMax(value = "5.0") BigDecimal priceRating,
        @Size(max = 500) String remarks
) {}

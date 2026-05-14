package com.stockpro.supplierservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateSupplierRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String contactPerson,
        @Email @Size(max = 100) String email,
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number") String phone,
        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid alternate phone number") String alternatePhone,
        @Size(max = 500) String address,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 100) String country,
        @Size(max = 20) String postalCode,
        @Size(max = 50) String taxNumber,
        @Size(max = 50) String gstNumber,
        @NotBlank @Size(max = 50) String paymentTerms,
        @Min(0) Integer leadTimeDays,
        @DecimalMin(value = "0.0") @DecimalMax(value = "5.0") BigDecimal rating,
        @Size(max = 1000) String notes
) {}

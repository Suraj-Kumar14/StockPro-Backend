package com.stockpro.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierFormRequest {

    private Long supplierId;

    @NotBlank(message = "Supplier name is required.")
    private String name;

    @NotBlank(message = "Contact person is required.")
    private String contactPerson;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email is invalid.")
    private String email;

    private String phone;
    private String address;

    @NotBlank(message = "City is required.")
    private String city;

    @NotBlank(message = "Country is required.")
    private String country;

    @NotBlank(message = "Tax ID is required.")
    private String taxId;

    @NotBlank(message = "Payment terms are required.")
    private String paymentTerms;

    @NotNull(message = "Lead time is required.")
    @Positive(message = "Lead time must be greater than zero.")
    private Integer leadTimeDays;

    private Double rating;
    private Boolean active = Boolean.TRUE;
}

package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SupplierResponse {

    private Long supplierId;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String taxId;
    private String paymentTerms;
    private Integer leadTimeDays;
    private Double rating;

    @JsonAlias({ "active", "isActive" })
    private Boolean active;

    private LocalDateTime createdAt;
}

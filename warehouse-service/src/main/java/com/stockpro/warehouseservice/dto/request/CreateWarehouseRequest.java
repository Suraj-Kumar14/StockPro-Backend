package com.stockpro.warehouseservice.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWarehouseRequest {
    @JsonAlias("warehouseName")
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @JsonAlias("warehouseCode")
    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank private String location;
    private String address;
    @NotBlank private String city;
    @NotBlank private String state;
    @NotBlank private String country;
    private Long managerId;
    @NotNull @Min(1) private Integer capacity;
    @Pattern(regexp = "^$|^[6-9][0-9]{9}$", message = "Phone must be a valid 10-digit Indian mobile number")
    private String phone;

    @JsonAlias("active")
    private Boolean isActive;
}

package com.stockpro.warehouseservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateWarehouseRequest {
    @NotBlank private String name;
    @NotBlank private String location;
    private String address;
    private Long managerId;
    @Min(1) private Integer capacity;
    private String phone;
    private Boolean isActive;
}

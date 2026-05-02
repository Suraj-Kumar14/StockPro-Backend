package com.stockpro.warehouseservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateWarehouseRequest {
    @NotBlank private String name;
    @NotBlank private String code;
    @NotBlank private String location;
    private String address;
    private Long managerId;
    @Min(1) private Integer capacity;
    private String phone;
}

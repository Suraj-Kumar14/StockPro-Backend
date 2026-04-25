package com.stockpro.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseFormRequest {

    private Long warehouseId;

    @NotBlank(message = "Warehouse name is required.")
    private String name;

    @NotBlank(message = "Location is required.")
    private String location;

    @NotBlank(message = "Address is required.")
    private String address;

    @Positive(message = "Manager ID must be greater than zero.")
    private Long managerId;

    @Positive(message = "Capacity must be greater than zero.")
    private Integer capacity;

    private String phone;
    private Boolean active = Boolean.TRUE;
}

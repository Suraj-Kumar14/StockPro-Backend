package com.stockpro.warehouse.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body used to update warehouse master data.")
public class UpdateWarehouseRequest {

    @NotBlank(message = "Warehouse name is required.")
    @Schema(example = "Central Distribution Hub")
    private String name;

    @NotBlank(message = "Warehouse location is required.")
    @Schema(example = "Bengaluru")
    private String location;

    @NotBlank(message = "Warehouse address is required.")
    @Schema(example = "Plot 12, Electronics City Phase 1, Bengaluru")
    private String address;

    @Positive(message = "managerId must be greater than zero.")
    @Schema(example = "101")
    private Long managerId;

    @Positive(message = "capacity must be greater than zero.")
    @Schema(example = "7000")
    private Integer capacity;

    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "phone must be a valid contact number.")
    @Schema(example = "+91-9988776655")
    private String phone;

    @Schema(example = "true")
    private Boolean isActive;
}

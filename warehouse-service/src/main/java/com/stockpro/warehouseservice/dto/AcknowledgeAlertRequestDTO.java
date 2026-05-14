package com.stockpro.warehouseservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AcknowledgeAlertRequestDTO {

    @NotBlank(message = "Acknowledged by is required")
    private String acknowledgedBy;
}

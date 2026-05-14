package com.stockpro.warehouseservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlertResponseDTO {

    private Long alertId;
    private Long warehouseId;
    private Long productId;
    private String alertType;
    private Integer currentQuantity;
    private Integer thresholdValue;
    private Boolean active;
    private Boolean acknowledged;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

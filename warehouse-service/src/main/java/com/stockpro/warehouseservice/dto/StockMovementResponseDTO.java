package com.stockpro.warehouseservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementResponseDTO {

    private Long movementId;
    private Long warehouseId;
    private Long productId;
    private String movementType;
    private Integer quantityChanged;
    private Integer previousQuantity;
    private Integer newQuantity;
    private Long relatedWarehouseId;
    private String reason;
    private LocalDateTime createdAt;
}

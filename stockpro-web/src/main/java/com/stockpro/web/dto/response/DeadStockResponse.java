package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeadStockResponse {

    private Long productId;
    private String productName;
    private Long warehouseId;
    private LocalDateTime lastMovementDate;
    private Long daysWithoutMovement;
}

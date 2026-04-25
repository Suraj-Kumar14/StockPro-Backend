package com.stockpro.web.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stockpro.web.dto.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MovementResponse {

    private Long movementId;
    private Long productId;
    private Long warehouseId;
    private MovementType movementType;
    private BigDecimal quantity;
    private Long referenceId;
    private String referenceType;
    private BigDecimal unitCost;
    private Long performedBy;
    private String notes;
    private LocalDateTime movementDate;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
}

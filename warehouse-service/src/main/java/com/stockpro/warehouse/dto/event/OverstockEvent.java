package com.stockpro.warehouse.dto.event;

import com.stockpro.warehouse.enums.AlertSeverity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class OverstockEvent {

    private Long recipientId;
    private Long productId;
    private Long warehouseId;
    private String productName;
    private String warehouseName;
    private BigDecimal quantity;
    private BigDecimal maxStockLevel;
    private AlertSeverity severity;
    private LocalDateTime eventTime;
}

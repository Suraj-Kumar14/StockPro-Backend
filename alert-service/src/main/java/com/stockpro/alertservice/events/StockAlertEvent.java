package com.stockpro.alertservice.events;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class StockAlertEvent {
    private String eventId;
    private String eventType;
    private Long productId;
    private String productSku;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    @JsonAlias({"currentQty", "currentQuantity"})
    private BigDecimal currentQuantity;
    @JsonAlias({"availableQuantity"})
    private BigDecimal availableQuantity;
    private BigDecimal reorderLevel;
    @JsonAlias({"maxLevel", "maxStockLevel"})
    private BigDecimal maxStockLevel;
    private String referenceType;
    private String referenceId;
    private String referenceNumber;
    private String sourceService;
    private String correlationId;
}

package com.stockpro.reportservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InventoryTurnoverReportResponse(
        LocalDate from,
        LocalDate to,
        Long warehouseId,
        BigDecimal cogs,
        BigDecimal averageInventoryValue,
        BigDecimal turnoverRate,
        String note,
        List<InventoryTurnoverProductItem> productTurnover) {
}

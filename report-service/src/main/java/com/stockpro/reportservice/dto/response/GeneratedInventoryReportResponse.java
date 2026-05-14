package com.stockpro.reportservice.dto.response;

import java.util.List;

public record GeneratedInventoryReportResponse(
        InventoryValuationResponse valuation,
        List<WarehouseValuationItem> stockValueByWarehouse,
        InventoryTurnoverReportResponse turnover,
        List<LowStockReportItem> lowStock,
        List<ProductMovementSummaryResponse> movementVelocity,
        List<TopMovingProductResponse> topMovingProducts,
        List<SlowMovingProductResponse> slowMovingProducts,
        List<DeadStockResponse> deadStock,
        PurchaseSummaryResponse poSummary,
        List<String> warnings) {
}

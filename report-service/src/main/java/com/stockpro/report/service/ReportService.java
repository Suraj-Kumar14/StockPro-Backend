package com.stockpro.report.service;

import com.stockpro.report.dto.request.GenerateReportRequest;
import com.stockpro.report.dto.request.ReportFilterRequest;
import com.stockpro.report.dto.request.TakeSnapshotRequest;
import com.stockpro.report.dto.response.DeadStockResponse;
import com.stockpro.report.dto.response.GeneratedReportResponse;
import com.stockpro.report.dto.response.InventorySnapshotResponse;
import com.stockpro.report.dto.response.InventoryTurnoverResponse;
import com.stockpro.report.dto.response.LowStockReportResponse;
import com.stockpro.report.dto.response.PurchaseOrderSummaryResponse;
import com.stockpro.report.dto.response.StockMovementSummaryResponse;
import com.stockpro.report.dto.response.SlowMovingProductResponse;
import com.stockpro.report.dto.response.TopMovingProductResponse;
import com.stockpro.report.dto.response.TotalStockValueResponse;
import com.stockpro.report.dto.response.WarehouseStockValueResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ReportService {

    InventorySnapshotResponse takeSnapshot(TakeSnapshotRequest request);

    void takeDailySnapshot();

    TotalStockValueResponse getTotalStockValue(LocalDate snapshotDate);

    List<WarehouseStockValueResponse> getStockValueByWarehouse(LocalDate snapshotDate);

    List<InventoryTurnoverResponse> getInventoryTurnover(ReportFilterRequest request);

    Page<LowStockReportResponse> getLowStockReport(ReportFilterRequest request);

    StockMovementSummaryResponse getStockMovementSummary(ReportFilterRequest request);

    Page<TopMovingProductResponse> getTopMovingProducts(ReportFilterRequest request);

    Page<SlowMovingProductResponse> getSlowMovingProducts(ReportFilterRequest request);

    Page<DeadStockResponse> getDeadStock(ReportFilterRequest request);

    PurchaseOrderSummaryResponse getPOSummary(ReportFilterRequest request);

    GeneratedReportResponse generateInventoryReport(GenerateReportRequest request);
}

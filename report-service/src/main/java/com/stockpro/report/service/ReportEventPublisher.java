package com.stockpro.report.service;

import com.stockpro.report.dto.event.InventorySnapshotCompletedEvent;
import com.stockpro.report.dto.event.ReportGenerationCompletedEvent;
import com.stockpro.report.dto.event.ReportGenerationRequestedEvent;

public interface ReportEventPublisher {

    void publishSnapshotCompleted(InventorySnapshotCompletedEvent event);

    void publishReportGenerationRequested(ReportGenerationRequestedEvent event);

    void publishReportGenerationCompleted(ReportGenerationCompletedEvent event);
}

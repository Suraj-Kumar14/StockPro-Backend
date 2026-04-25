package com.stockpro.report.scheduler;

import com.stockpro.report.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventorySnapshotScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventorySnapshotScheduler.class);

    private final ReportService reportService;

    public InventorySnapshotScheduler(ReportService reportService) {
        this.reportService = reportService;
    }

    @Scheduled(cron = "${report.snapshot.cron:0 0 0 * * *}")
    public void takeDailySnapshot() {
        LOGGER.info("Starting scheduled inventory snapshot job.");
        try {
            reportService.takeDailySnapshot();
            LOGGER.info("Completed scheduled inventory snapshot job.");
        } catch (RuntimeException exception) {
            LOGGER.error("Scheduled inventory snapshot job failed: {}", exception.getMessage(), exception);
            throw exception;
        }
    }
}

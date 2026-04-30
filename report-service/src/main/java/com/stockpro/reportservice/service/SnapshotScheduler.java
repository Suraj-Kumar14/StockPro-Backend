package com.stockpro.reportservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SnapshotScheduler {

    @Autowired
    private ReportService reportService;

    // Daily snapshot at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void takeDailySnapshot() {
        log.info("Running daily inventory snapshot job");
        reportService.takeDailySnapshot();
        log.info("Daily snapshot job completed");
    }

    // Low stock check every 15 minutes
    @Scheduled(fixedRate = 900000)
    public void checkLowStock() {
        log.debug("Refreshing low stock analytics view");
        reportService.getLowStockReport(10);
    }

    // Overdue PO alert at 9:00 AM daily
    @Scheduled(cron = "0 0 9 * * *")
    public void checkOverduePOs() {
        log.info("Refreshing overdue PO summary analytics");
        reportService.getPOSummary(java.time.LocalDate.now().minusDays(30), java.time.LocalDate.now());
    }
}

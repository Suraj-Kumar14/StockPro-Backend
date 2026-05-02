package com.stockpro.reportservice.scheduler;

import com.stockpro.reportservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySnapshotScheduler {

    private final ReportService reportService;

    @Value("${report.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    @Scheduled(cron = "0 0 0 * * *")
    public void createDailySnapshot() {
        if (!schedulerEnabled) {
            log.info("Daily snapshot scheduler is disabled. Skipping scheduled snapshot run.");
            return;
        }
        reportService.createDailyInventorySnapshot();
    }
}

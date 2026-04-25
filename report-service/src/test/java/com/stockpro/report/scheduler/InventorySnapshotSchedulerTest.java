package com.stockpro.report.scheduler;

import static org.mockito.Mockito.verify;

import com.stockpro.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventorySnapshotSchedulerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private InventorySnapshotScheduler inventorySnapshotScheduler;

    @Test
    void schedulerShouldTriggerDailySnapshot() {
        inventorySnapshotScheduler.takeDailySnapshot();
        verify(reportService).takeDailySnapshot();
    }
}

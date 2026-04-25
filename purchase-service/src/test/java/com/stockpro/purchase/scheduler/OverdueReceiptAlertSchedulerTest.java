package com.stockpro.purchase.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.purchase.client.WarehouseClient;
import com.stockpro.purchase.dto.WarehouseSummaryResponse;
import com.stockpro.purchase.dto.event.OverdueReceiptEvent;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.entity.PurchaseOrderStatus;
import com.stockpro.purchase.publisher.AlertEventPublisher;
import com.stockpro.purchase.repository.PurchaseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OverdueReceiptAlertSchedulerTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private WarehouseClient warehouseClient;

    @Mock
    private AlertEventPublisher alertEventPublisher;

    private OverdueReceiptAlertScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OverdueReceiptAlertScheduler(purchaseRepository, warehouseClient, alertEventPublisher);
        ReflectionTestUtils.setField(scheduler, "overdueAlertEnabled", true);
    }

    @Test
    void publishOverdueReceiptAlertsShouldSendAlertEventsForApprovedOverdueOrders() {
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .poId(9001L)
                .supplierId(7L)
                .warehouseId(3L)
                .createdById(55L)
                .status(PurchaseOrderStatus.APPROVED)
                .totalAmount(new BigDecimal("1205.00"))
                .orderDate(LocalDate.now().minusDays(10))
                .expectedDate(LocalDate.now().minusDays(4))
                .receivedDate(null)
                .build();

        when(purchaseRepository.findByStatusAndExpectedDateBeforeAndReceivedDateIsNull(
                PurchaseOrderStatus.APPROVED,
                LocalDate.now())).thenReturn(List.of(purchaseOrder));
        when(warehouseClient.getWarehouseById(3L)).thenReturn(WarehouseSummaryResponse.builder()
                .warehouseId(3L)
                .managerId(101L)
                .name("Central Warehouse")
                .isActive(Boolean.TRUE)
                .build());

        scheduler.publishOverdueReceiptAlerts();

        verify(alertEventPublisher).publishOverdueReceiptEvent(any(OverdueReceiptEvent.class));
    }
}

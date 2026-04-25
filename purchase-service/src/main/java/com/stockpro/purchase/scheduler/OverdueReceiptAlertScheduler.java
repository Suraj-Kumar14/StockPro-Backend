package com.stockpro.purchase.scheduler;

import com.stockpro.purchase.client.WarehouseClient;
import com.stockpro.purchase.dto.WarehouseSummaryResponse;
import com.stockpro.purchase.dto.event.OverdueReceiptEvent;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.entity.PurchaseOrderStatus;
import com.stockpro.purchase.enums.AlertSeverity;
import com.stockpro.purchase.publisher.AlertEventPublisher;
import com.stockpro.purchase.repository.PurchaseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OverdueReceiptAlertScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverdueReceiptAlertScheduler.class);

    private final PurchaseRepository purchaseRepository;
    private final WarehouseClient warehouseClient;
    private final AlertEventPublisher alertEventPublisher;

    @Value("${purchase.alert.overdue-receipt.enabled:false}")
    private boolean overdueAlertEnabled;

    public OverdueReceiptAlertScheduler(PurchaseRepository purchaseRepository,
            WarehouseClient warehouseClient,
            AlertEventPublisher alertEventPublisher) {
        this.purchaseRepository = purchaseRepository;
        this.warehouseClient = warehouseClient;
        this.alertEventPublisher = alertEventPublisher;
    }

    @Scheduled(cron = "${purchase.alert.overdue-receipt.cron:0 0 9 * * *}")
    public void publishOverdueReceiptAlerts() {
        if (!overdueAlertEnabled) {
            return;
        }

        LocalDate today = LocalDate.now();
        purchaseRepository.findByStatusAndExpectedDateBeforeAndReceivedDateIsNull(PurchaseOrderStatus.APPROVED, today)
                .forEach(purchaseOrder -> publishOverdueReceiptAlert(purchaseOrder, today));
    }

    private void publishOverdueReceiptAlert(PurchaseOrder purchaseOrder, LocalDate today) {
        Long recipientId = resolveRecipientId(purchaseOrder);
        if (recipientId == null) {
            LOGGER.warn("Skipping overdue receipt alert because no recipient was resolved for poId={}", purchaseOrder.getPoId());
            return;
        }

        alertEventPublisher.publishOverdueReceiptEvent(OverdueReceiptEvent.builder()
                .recipientId(recipientId)
                .purchaseOrderId(purchaseOrder.getPoId())
                .supplierId(purchaseOrder.getSupplierId())
                .warehouseId(purchaseOrder.getWarehouseId())
                .expectedDate(purchaseOrder.getExpectedDate())
                .severity(resolveSeverity(purchaseOrder.getExpectedDate(), today))
                .eventTime(LocalDateTime.now())
                .build());
    }

    private Long resolveRecipientId(PurchaseOrder purchaseOrder) {
        try {
            WarehouseSummaryResponse warehouse = warehouseClient.getWarehouseById(purchaseOrder.getWarehouseId());
            if (warehouse != null && warehouse.getManagerId() != null && warehouse.getManagerId() > 0) {
                return warehouse.getManagerId();
            }
        } catch (Exception exception) {
            LOGGER.warn("Unable to resolve warehouse manager for overdue alert poId={}: {}", purchaseOrder.getPoId(), exception.getMessage());
        }

        return purchaseOrder.getCreatedById();
    }

    private AlertSeverity resolveSeverity(LocalDate expectedDate, LocalDate today) {
        long daysOverdue = ChronoUnit.DAYS.between(expectedDate, today);
        return daysOverdue >= 3 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
    }
}

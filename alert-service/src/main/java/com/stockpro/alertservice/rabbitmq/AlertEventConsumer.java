package com.stockpro.alertservice.rabbitmq;

import com.stockpro.alertservice.dto.AlertRequestDTO;
import com.stockpro.alertservice.entity.AlertType;
import com.stockpro.alertservice.entity.Severity;
import com.stockpro.alertservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlertEventConsumer {

    private final AlertService alertService;

    @RabbitListener(queues = RabbitMQConfig.LOW_STOCK_QUEUE)
    public void handleLowStock(LowStockEvent event) {
        log.info("Received LOW_STOCK event product={} warehouse={} qty={}",
                event.getProductId(), event.getWarehouseId(), event.getCurrentQuantity());

        try {
            AlertRequestDTO dto = alertService.buildLowStockAlert(
                    1L,
                    event.getProductId(),
                    event.getWarehouseId(),
                    event.getProductName(),
                    event.getWarehouseName(),
                    event.getCurrentQuantity(),
                    event.getReorderLevel());
            alertService.createAlert(dto);
        } catch (Exception ex) {
            log.error("Failed to process LOW_STOCK event", ex);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.OVERSTOCK_QUEUE)
    public void handleOverstock(OverstockEvent event) {
        log.info("Received OVERSTOCK event product={} warehouse={} qty={}",
                event.getProductId(), event.getWarehouseId(), event.getCurrentQuantity());

        try {
            AlertRequestDTO dto = alertService.buildOverstockAlert(
                    1L,
                    event.getProductId(),
                    event.getWarehouseId(),
                    event.getProductName(),
                    event.getWarehouseName(),
                    event.getCurrentQuantity(),
                    event.getMaxStockLevel());
            alertService.createAlert(dto);
        } catch (Exception ex) {
            log.error("Failed to process OVERSTOCK event", ex);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PO_PENDING_QUEUE)
    public void handlePoPending(POPendingEvent event) {
        log.info("Received PO_PENDING event poId={} requestedBy={}",
                event.getPoId(), event.getRequestedByUserId());

        try {
            AlertRequestDTO dto = alertService.buildPendingPoAlert(
                    event.getRequestedByUserId(),
                    event.getPoId(),
                    String.format("PO #%d is pending approval. Expected delivery: %s.",
                            event.getPoId(), event.getExpectedDate()));
            alertService.createAlert(dto);
        } catch (Exception ex) {
            log.error("Failed to process PO_PENDING event", ex);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PO_APPROVED_QUEUE)
    public void handlePoApproved(POApprovedEvent event) {
        log.info("Received PO_APPROVED event poId={} approver={}",
                event.getPoId(), event.getApprovedByUserId());

        try {
            AlertRequestDTO dto = new AlertRequestDTO();
            dto.setRecipientId(event.getApprovedByUserId());
            dto.setType(AlertType.SYSTEM);
            dto.setSeverity(Severity.INFO);
            dto.setTitle("Purchase Order Approved");
            dto.setMessage(String.format(
                    "PO #%d has been approved. Total amount: %.2f. Expected delivery: %s.",
                    event.getPoId(), event.getTotalAmount(), event.getExpectedDate()));
            dto.setRelatedPurchaseOrderId(event.getPoId());
            dto.setChannel("IN_APP");
            alertService.createAlert(dto);
        } catch (Exception ex) {
            log.error("Failed to process PO_APPROVED event", ex);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PO_OVERDUE_QUEUE)
    public void handlePoOverdue(POOverdueEvent event) {
        log.info("Received PO_OVERDUE event poId={} daysOverdue={}",
                event.getPoId(), event.getDaysOverdue());

        try {
            AlertRequestDTO dto = alertService.buildOverdueReceiptAlert(
                    1L,
                    event.getPoId(),
                    String.format("PO #%d is overdue by %d days. Expected delivery was %s.",
                            event.getPoId(), event.getDaysOverdue(), event.getExpectedDate()));
            alertService.createAlert(dto);
        } catch (Exception ex) {
            log.error("Failed to process PO_OVERDUE event", ex);
        }
    }
}

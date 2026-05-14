package com.stockpro.movementservice.listener;

import com.stockpro.movementservice.dto.request.CreateMovementFromEventRequest;
import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementReasonCode;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import com.stockpro.movementservice.events.WarehouseStockEvent;
import com.stockpro.movementservice.service.MovementService;
import java.math.BigDecimal;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventConsumer {

    private final MovementService movementService;

    @RabbitListener(queues = "${stockpro.rabbitmq.stock.movement.queue}")
    public void consumeStockEvent(WarehouseStockEvent event) {
        try {
            CreateMovementFromEventRequest request = map(event);
            if (request == null) {
                log.info("Skipped warehouse stock event type={}", event.eventType());
                return;
            }
            movementService.createMovementFromEvent(request);
        } catch (Exception ex) {
            log.error("Failed to consume warehouse stock event eventId={} type={} error={}",
                    event.eventId(), event.eventType(), ex.getMessage(), ex);
        }
    }

    private CreateMovementFromEventRequest map(WarehouseStockEvent event) {
        String eventType = event.eventType() == null ? "" : event.eventType().toUpperCase(Locale.ROOT);
        return switch (eventType) {
            case "STOCK_RECEIVED" -> new CreateMovementFromEventRequest(
                    event.eventId(), event.eventType(), event.productId(), null, null,
                    event.warehouseId(), null, null, null, null,
                    MovementType.STOCK_IN, MovementDirection.IN,
                    decimal(event.operationQuantity()), BigDecimal.ZERO, decimal(event.balanceAfter()),
                    parseReferenceType(event.referenceType(), ReferenceType.GRN), event.referenceId(), event.referenceId(),
                    event.actorId(), null, MovementReasonCode.PURCHASE_RECEIPT, event.notes(), event.eventTime(),
                    "warehouse-service", event.eventId());
            case "STOCK_ISSUED" -> new CreateMovementFromEventRequest(
                    event.eventId(), event.eventType(), event.productId(), null, null,
                    event.warehouseId(), null, null, null, null,
                    MovementType.STOCK_OUT, MovementDirection.OUT,
                    decimal(event.operationQuantity()), BigDecimal.ZERO, decimal(event.balanceAfter()),
                    parseReferenceType(event.referenceType(), ReferenceType.SYSTEM), event.referenceId(), event.referenceId(),
                    event.actorId(), null, MovementReasonCode.STOCK_ISSUE, event.notes(), event.eventTime(),
                    "warehouse-service", event.eventId());
            case "STOCK_TRANSFERRED" -> new CreateMovementFromEventRequest(
                    event.eventId(), event.eventType(), event.productId(), null, null,
                    event.warehouseId(), null, null, event.sourceWarehouseId(), event.destinationWarehouseId(),
                    MovementType.TRANSFER_OUT, MovementDirection.OUT,
                    decimal(event.operationQuantity()), BigDecimal.ZERO, decimal(event.balanceAfter()),
                    ReferenceType.TRANSFER, event.referenceId(), event.referenceId(),
                    event.actorId(), null, MovementReasonCode.TRANSFER_OUT, event.notes(), event.eventTime(),
                    "warehouse-service", event.eventId());
            case "STOCK_ADJUSTED" -> {
                int delta = event.operationQuantity() != null ? event.operationQuantity() : 0;
                if (delta == 0) {
                    yield null;
                }
                yield new CreateMovementFromEventRequest(
                        event.eventId(), event.eventType(), event.productId(), null, null,
                        event.warehouseId(), null, null, null, null,
                        MovementType.ADJUSTMENT, delta > 0 ? MovementDirection.IN : MovementDirection.OUT,
                        BigDecimal.valueOf(Math.abs(delta)), BigDecimal.ZERO, decimal(event.balanceAfter()),
                        ReferenceType.ADJUSTMENT, event.referenceId(), event.referenceId(),
                        event.actorId(), null, MovementReasonCode.SYSTEM_CORRECTION, event.notes(), event.eventTime(),
                        "warehouse-service", event.eventId());
            }
            case "STOCK_RESERVED" -> new CreateMovementFromEventRequest(
                    event.eventId(), event.eventType(), event.productId(), null, null,
                    event.warehouseId(), null, null, null, null,
                    MovementType.RESERVATION, MovementDirection.NEUTRAL,
                    decimal(event.operationQuantity()), BigDecimal.ZERO, decimal(event.balanceAfter()),
                    parseReferenceType(event.referenceType(), ReferenceType.SYSTEM), event.referenceId(), event.referenceId(),
                    event.actorId(), null, MovementReasonCode.OTHER, event.notes(), event.eventTime(),
                    "warehouse-service", event.eventId());
            case "STOCK_RESERVATION_RELEASED" -> new CreateMovementFromEventRequest(
                    event.eventId(), event.eventType(), event.productId(), null, null,
                    event.warehouseId(), null, null, null, null,
                    MovementType.RESERVATION_RELEASE, MovementDirection.NEUTRAL,
                    decimal(event.operationQuantity()), BigDecimal.ZERO, decimal(event.balanceAfter()),
                    parseReferenceType(event.referenceType(), ReferenceType.SYSTEM), event.referenceId(), event.referenceId(),
                    event.actorId(), null, MovementReasonCode.OTHER, event.notes(), event.eventTime(),
                    "warehouse-service", event.eventId());
            default -> null;
        };
    }

    private BigDecimal decimal(Integer value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value);
    }

    private ReferenceType parseReferenceType(String value, ReferenceType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return ReferenceType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return fallback;
        }
    }
}

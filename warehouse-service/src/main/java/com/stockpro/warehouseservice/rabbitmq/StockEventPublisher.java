package com.stockpro.warehouseservice.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class StockEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${stockpro.rabbitmq.warehouse.exchange}")
    private String exchange;

    @Value("${stockpro.rabbitmq.warehouse.routing.stock-low}")
    private String lowStockRoutingKey;

    @Value("${stockpro.rabbitmq.warehouse.routing.stock-overstock}")
    private String overstockRoutingKey;

    @Value("${stockpro.rabbitmq.warehouse.routing.stock-received}")
    private String goodsReceivedRoutingKey;

    // Called when stock drops below reorder level
    public void publishLowStockEvent(Long productId, Long warehouseId,
                                     Integer currentQty, Integer reorderLevel) {
        Map<String, Object> event = Map.of(
                "productId",    productId,
                "warehouseId",  warehouseId,
                "currentQty",   currentQty,
                "reorderLevel", reorderLevel,
                "eventType",    "LOW_STOCK"
        );
        rabbitTemplate.convertAndSend(
                exchange,
                lowStockRoutingKey,
                event
        );
        log.info("Published LOW_STOCK event for product {} in warehouse {}",
                productId, warehouseId);
    }

    // Called when stock exceeds max stock level
    public void publishOverstockEvent(Long productId, Long warehouseId,
                                      Integer currentQty, Integer maxLevel) {
        Map<String, Object> event = Map.of(
                "productId",   productId,
                "warehouseId", warehouseId,
                "currentQty",  currentQty,
                "maxLevel",    maxLevel,
                "eventType",   "OVERSTOCK"
        );
        rabbitTemplate.convertAndSend(
                exchange,
                overstockRoutingKey,
                event
        );
        log.info("Published OVERSTOCK event for product {} in warehouse {}",
                productId, warehouseId);
    }

    // Called when goods are received (PO receipt)
    public void publishGoodsReceivedEvent(Long productId, Long warehouseId,
                                          Integer receivedQty, Long poId) {
        Map<String, Object> event = Map.of(
                "productId",   productId,
                "warehouseId", warehouseId,
                "receivedQty", receivedQty,
                "poId",        poId,
                "eventType",   "GOODS_RECEIVED"
        );
        rabbitTemplate.convertAndSend(
                exchange,
                goodsReceivedRoutingKey,
                event
        );
        log.info("Published GOODS_RECEIVED event for product {} in warehouse {}",
                productId, warehouseId);
    }
}

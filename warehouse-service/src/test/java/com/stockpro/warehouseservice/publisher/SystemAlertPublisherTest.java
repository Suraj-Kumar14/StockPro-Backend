package com.stockpro.warehouseservice.publisher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.stockpro.warehouseservice.events.SystemAlertEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SystemAlertPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private SystemAlertPublisher systemAlertPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(systemAlertPublisher, "exchange", "stockpro.alerts.exchange");
    }

    @Test
    void publish_shouldSendEventToConfiguredExchange() {
        SystemAlertEvent event = buildEvent();

        systemAlertPublisher.publish("warehouse.low-stock", event);

        verify(rabbitTemplate).convertAndSend("stockpro.alerts.exchange", "warehouse.low-stock", event);
    }

    @Test
    void publish_shouldSwallowRabbitFailures() {
        SystemAlertEvent event = buildEvent();
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate)
                .convertAndSend("stockpro.alerts.exchange", "warehouse.low-stock", event);

        assertDoesNotThrow(() -> systemAlertPublisher.publish("warehouse.low-stock", event));
        verify(rabbitTemplate).convertAndSend("stockpro.alerts.exchange", "warehouse.low-stock", event);
    }

    private SystemAlertEvent buildEvent() {
        return SystemAlertEvent.builder()
                .eventId("evt-1")
                .eventType("LOW_STOCK_DETECTED")
                .severity("HIGH")
                .title("Low stock")
                .message("Warehouse item reached reorder level")
                .recipientRoles(List.of("ADMIN"))
                .recipientIds(List.of(10L))
                .actionUrl("/alerts/evt-1")
                .referenceType("PRODUCT")
                .referenceId("100")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .sourceService("warehouse-service")
                .correlationId("corr-1")
                .build();
    }
}

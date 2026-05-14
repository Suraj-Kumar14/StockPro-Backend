package com.stockpro.purchaseservice.publisher;

import com.stockpro.purchaseservice.events.SystemAlertEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SystemAlertPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private SystemAlertPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SystemAlertPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchange", "alert.exchange");
    }

    @Test
    void publish_shouldSendEventToRabbitMq() {
        SystemAlertEvent event = sampleEvent();

        publisher.publish("alert.routing", event);

        verify(rabbitTemplate).convertAndSend("alert.exchange", "alert.routing", event);
    }

    @Test
    void publish_shouldSwallowBrokerExceptions() {
        SystemAlertEvent event = sampleEvent();
        doThrow(new RuntimeException("broker unavailable"))
                .when(rabbitTemplate)
                .convertAndSend("alert.exchange", "alert.routing", event);

        publisher.publish("alert.routing", event);

        verify(rabbitTemplate).convertAndSend("alert.exchange", "alert.routing", event);
    }

    private SystemAlertEvent sampleEvent() {
        return SystemAlertEvent.builder()
                .eventId("evt-1")
                .eventType("SYSTEM_ALERT")
                .severity("HIGH")
                .title("Purchase warning")
                .message("Something happened")
                .recipientRoles(List.of("ADMIN"))
                .recipientIds(List.of(1L))
                .actionUrl("/alerts/1")
                .referenceType("PURCHASE_ORDER")
                .referenceId("PO-1")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .sourceService("purchase-service")
                .correlationId("corr-1")
                .build();
    }
}

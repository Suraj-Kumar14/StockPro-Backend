package com.stockpro.movementservice.publisher;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.stockpro.movementservice.events.SystemAlertEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SystemAlertPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private SystemAlertPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SystemAlertPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchange", "stockpro.alerts.exchange");
    }

    @Test
    void publish_shouldSendAlertToRabbitMq() {
        SystemAlertEvent event = sampleEvent();

        publisher.publish("movement.alert.created", event);

        verify(rabbitTemplate).convertAndSend("stockpro.alerts.exchange", "movement.alert.created", event);
    }

    @Test
    void publish_shouldSwallowRabbitExceptionsAfterLogging() {
        SystemAlertEvent event = sampleEvent();
        doThrow(new RuntimeException("broker down"))
                .when(rabbitTemplate)
                .convertAndSend("stockpro.alerts.exchange", "movement.alert.failed", event);

        publisher.publish("movement.alert.failed", event);

        verify(rabbitTemplate).convertAndSend("stockpro.alerts.exchange", "movement.alert.failed", event);
    }

    private SystemAlertEvent sampleEvent() {
        return SystemAlertEvent.builder()
                .eventId("evt-1")
                .eventType("movement.alert")
                .severity("WARN")
                .title("Movement Alert")
                .message("Movement threshold reached")
                .userMessage("Check stock movement")
                .technicalDetails("movement-service test")
                .recipientRoles(List.of("ADMIN"))
                .recipientIds(List.of(10L))
                .actionUrl("/movements")
                .referenceType("MOVEMENT")
                .referenceId("1")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .sourceService("movement-service")
                .correlationId("corr-1")
                .build();
    }
}

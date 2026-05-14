package com.stockpro.authservice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.stockpro.authservice.service.OtpNotificationEvent;
import com.stockpro.authservice.service.RabbitMqOtpEventPublisher;

@ExtendWith(MockitoExtension.class)
class RabbitMqOtpEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMqOtpEventPublisher publisher;

    private OtpNotificationEvent event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "exchange", "auth.exchange");
        ReflectionTestUtils.setField(publisher, "routingKey", "auth.otp");
        event = new OtpNotificationEvent(
                "event-1",
                "user@example.com",
                "123456",
                "SIGNUP_VERIFICATION",
                "User Example",
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now());
    }

    @Test
    void publish_shouldReturnTrue_whenRabbitTemplateSucceeds() {
        boolean published = publisher.publish(event);

        assertTrue(published);
        verify(rabbitTemplate).convertAndSend("auth.exchange", "auth.otp", event);
    }

    @Test
    void publish_shouldReturnFalse_whenRabbitTemplateThrowsAmqpException() {
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate)
                .convertAndSend("auth.exchange", "auth.otp", event);

        boolean published = publisher.publish(event);

        assertFalse(published);
        verify(rabbitTemplate).convertAndSend("auth.exchange", "auth.otp", event);
    }
}

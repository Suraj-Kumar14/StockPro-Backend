package com.stockpro.alertservice.listener;

import com.stockpro.alertservice.events.OtpNotificationEvent;
import com.stockpro.alertservice.mail.OtpEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthOtpEventConsumer {

    private final OtpEmailService otpEmailService;

    @RabbitListener(queues = "${stockpro.rabbitmq.auth.otp.queue}")
    public void consumeOtpNotification(OtpNotificationEvent event) {
        log.info("OTP email consumed email={} purpose={} eventId={}", event.email(), event.purpose(), event.eventId());
        otpEmailService.sendOtpEmail(event);
        log.info("OTP email sent email={} purpose={} eventId={}", event.email(), event.purpose(), event.eventId());
    }
}

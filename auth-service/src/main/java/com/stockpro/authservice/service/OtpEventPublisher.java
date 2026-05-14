package com.stockpro.authservice.service;

public interface OtpEventPublisher {
    boolean publish(OtpNotificationEvent event);
}

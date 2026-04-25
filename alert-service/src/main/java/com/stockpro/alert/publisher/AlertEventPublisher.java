package com.stockpro.alert.publisher;

import com.stockpro.alert.dto.event.EmailAlertEvent;

public interface AlertEventPublisher {

    void publishEmailAlert(EmailAlertEvent event);
}

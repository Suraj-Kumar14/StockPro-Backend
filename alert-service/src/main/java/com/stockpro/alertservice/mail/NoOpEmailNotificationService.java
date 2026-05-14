package com.stockpro.alertservice.mail;

import com.stockpro.alertservice.entity.Alert;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoOpEmailNotificationService implements EmailNotificationService {

    @Override
    public boolean sendCriticalAlertEmail(Alert alert, String recipientEmail) {
        log.info("Skipping email delivery for alert {} because SMTP or recipient lookup is not configured", alert.getAlertId());
        return false;
    }
}

package com.stockpro.alertservice.mail;

import com.stockpro.alertservice.entity.Alert;

public interface EmailNotificationService {
    boolean sendCriticalAlertEmail(Alert alert, String recipientEmail);
}

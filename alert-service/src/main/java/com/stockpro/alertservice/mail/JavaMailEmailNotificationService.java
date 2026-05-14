package com.stockpro.alertservice.mail;

import com.stockpro.alertservice.entity.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Slf4j
@RequiredArgsConstructor
public class JavaMailEmailNotificationService implements EmailNotificationService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    @Override
    public boolean sendCriticalAlertEmail(Alert alert, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Cannot send email for alert {} because recipient email could not be resolved", alert.getAlertId());
            return false;
        }

        try {
            var mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipientEmail);
            helper.setSubject("[CRITICAL] " + alert.getTitle());
            helper.setText(buildHtml(alert), true);
            mailSender.send(mimeMessage);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email for alert {}", alert.getAlertId(), ex);
            return false;
        }
    }

    private String buildHtml(Alert alert) {
        return """
                <html>
                <body style="font-family:Arial,sans-serif;background:#f5f7fb;color:#1f2937;padding:24px;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border-radius:12px;border:1px solid #e5e7eb;overflow:hidden;">
                    <div style="background:#b91c1c;color:#ffffff;padding:20px 24px;">
                      <h2 style="margin:0;font-size:22px;">Critical StockPro Alert</h2>
                    </div>
                    <div style="padding:24px;">
                      <p style="margin:0 0 12px 0;"><strong>Alert:</strong> %s</p>
                      <p style="margin:0 0 12px 0;"><strong>Message:</strong> %s</p>
                      <p style="margin:0 0 12px 0;"><strong>Severity:</strong> %s</p>
                      <p style="margin:0;"><strong>Raised At:</strong> %s</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(alert.getTitle(), alert.getMessage(), alert.getSeverity(), alert.getCreatedAt());
    }
}

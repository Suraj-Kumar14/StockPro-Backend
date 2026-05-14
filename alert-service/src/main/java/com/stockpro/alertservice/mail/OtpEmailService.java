package com.stockpro.alertservice.mail;

import com.stockpro.alertservice.events.OtpNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpEmailService {

    private final JavaMailSender mailSender;

    @Value("${stockpro.alert.mail.from-address:noreply@stockpro.local}")
    private String fromAddress;

    public void sendOtpEmail(OtpNotificationEvent event) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(event.email());
            helper.setSubject(subjectFor(event.purpose()));
            helper.setText(buildHtml(event), true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send OTP email email={} purpose={} eventId={}",
                    event.email(), event.purpose(), event.eventId(), ex);
            throw new IllegalStateException("OTP email delivery failed", ex);
        }
    }

    private String subjectFor(String purpose) {
        return "PASSWORD_RESET".equalsIgnoreCase(purpose)
                ? "StockPro Password Reset OTP"
                : "StockPro Verify Your Account";
    }

    private String buildHtml(OtpNotificationEvent event) {
        String heading = "PASSWORD_RESET".equalsIgnoreCase(event.purpose()) ? "Password Reset" : "Account Verification";
        String recipient = event.recipientName() != null && !event.recipientName().isBlank() ? event.recipientName() : "StockPro User";
        return """
                <html>
                <body style="margin:0;padding:0;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table width="520" cellpadding="0" cellspacing="0" style="max-width:520px;width:100%%;background:#ffffff;border-radius:14px;overflow:hidden;">
                          <tr>
                            <td style="background:linear-gradient(135deg,#14532d,#0f766e);padding:24px 32px;color:#ffffff;">
                              <h1 style="margin:0;font-size:24px;">StockPro</h1>
                              <p style="margin:8px 0 0;font-size:13px;opacity:0.88;">Inventory Management System</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 8px;color:#334155;">Hello %s,</p>
                              <h2 style="margin:0 0 12px;color:#0f172a;">%s OTP</h2>
                              <p style="margin:0 0 24px;color:#475569;">Use the code below to continue your StockPro flow.</p>
                              <div style="display:inline-block;background:#0f766e;color:#ffffff;padding:18px 28px;border-radius:12px;font-size:32px;letter-spacing:10px;font-weight:700;">%s</div>
                              <p style="margin:24px 0 0;color:#64748b;font-size:13px;">This OTP expires at %s.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(recipient, heading, event.otpCode(), event.expiresAt());
    }
}

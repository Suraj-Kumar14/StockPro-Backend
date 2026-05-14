package com.stockpro.authservice.service;

import com.stockpro.authservice.exception.EmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Handles all outgoing emails for the auth-service.
 * Uses MimeMessage for HTML email support (Module 5: OTP template upgrade).
 */
@Service
public class OtpMailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public OtpMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ─── OTP Emails ──────────────────────────────────────────────────────────

    public void sendSignupOtp(String to, String otp) {
        String subject = "StockPro – Verify Your Account";
        sendHtml(to, subject, buildOtpEmailBody("Account Verification", otp));
    }

    public void sendPasswordResetOtp(String to, String otp) {
        String subject = "StockPro – Password Reset OTP";
        sendHtml(to, subject, buildOtpEmailBody("Password Reset", otp));
    }

    // ─── Account Status Emails (Module 2) ────────────────────────────────────

    public void sendAccountDeactivatedEmail(String to, String userName) {
        String subject = "StockPro – Account Deactivated";
        sendHtml(to, subject, buildStatusEmailBody(
                userName,
                "Account Deactivated",
                "Your StockPro account has been <strong style=\"color:#dc3545;\">deactivated</strong> by an administrator.",
                "You will not be able to log in until your account is reactivated. If you believe this is a mistake, please contact your system administrator.",
                "#dc3545",
                "#fff0f0"
        ));
    }

    public void sendAccountReactivatedEmail(String to, String userName) {
        String subject = "StockPro – Account Reactivated";
        sendHtml(to, subject, buildStatusEmailBody(
                userName,
                "Account Reactivated",
                "Your StockPro account has been <strong style=\"color:#28a745;\">reactivated</strong> by an administrator.",
                "You can now log in to StockPro and resume your work.",
                "#28a745",
                "#f0fff4"
        ));
    }

    // ─── HTML Builders ────────────────────────────────────────────────────────

    /**
     * Module 5 – Beautiful HTML OTP email (replaces plain text).
     */
    private String buildOtpEmailBody(String purpose, String otp) {
        return "<!DOCTYPE html>" +
               "<html lang=\"en\">" +
               "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>" +
               "<body style=\"margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;\">" +
               "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f6f8;padding:40px 20px;\">" +
               "    <tr><td align=\"center\">" +
               "      <table width=\"520\" cellpadding=\"0\" cellspacing=\"0\"" +
               "             style=\"background:#ffffff;border-radius:12px;box-shadow:0 4px 20px rgba(0,0,0,0.08);overflow:hidden;max-width:520px;width:100%;\">" +
               "        <!-- Header -->" +
               "        <tr>" +
               "          <td style=\"background:linear-gradient(135deg,#1a73e8 0%,#0d47a1 100%);padding:28px 32px;text-align:center;\">" +
               "            <h1 style=\"color:#ffffff;margin:0;font-size:26px;font-weight:700;letter-spacing:-0.5px;\">StockPro</h1>" +
               "            <p style=\"color:rgba(255,255,255,0.8);margin:6px 0 0;font-size:13px;\">Inventory Management System</p>" +
               "          </td>" +
               "        </tr>" +
               "        <!-- Body -->" +
               "        <tr>" +
               "          <td style=\"padding:36px 40px;text-align:center;\">" +
               "            <h2 style=\"color:#2c3e50;font-size:22px;margin-top:0;margin-bottom:8px;\">" + purpose + "</h2>" +
               "            <p style=\"color:#555;font-size:15px;line-height:1.6;margin:0 0 28px;\">" +
               "              Use the one-time password below to complete your verification." +
               "            </p>" +
               "            <!-- OTP Box -->" +
               "            <div style=\"display:inline-block;background:linear-gradient(135deg,#1a73e8,#0d47a1);" +
               "                        border-radius:12px;padding:20px 40px;margin:0 auto 28px;\">" +
               "              <span style=\"font-size:36px;font-weight:700;color:#ffffff;" +
               "                           letter-spacing:12px;font-family:'Courier New',monospace;\">" +
               "                " + otp +
               "              </span>" +
               "            </div>" +
               "            <p style=\"color:#888;font-size:13px;margin:0 0 8px;\">" +
               "              &#9200; This OTP will expire in <strong>10 minutes</strong>." +
               "            </p>" +
               "            <p style=\"color:#aaa;font-size:12px;margin:0;\">" +
               "              If you didn't request this, please ignore this email.<br/>" +
               "              Your account remains secure." +
               "            </p>" +
               "          </td>" +
               "        </tr>" +
               "        <!-- Footer -->" +
               "        <tr>" +
               "          <td style=\"background:#f8fafc;border-top:1px solid #eee;padding:16px 32px;text-align:center;\">" +
               "            <p style=\"color:#bbb;font-size:11px;margin:0;\">" +
               "              &copy; 2024 StockPro &bull; Do not reply to this email" +
               "            </p>" +
               "          </td>" +
               "        </tr>" +
               "      </table>" +
               "    </td></tr>" +
               "  </table>" +
               "</body></html>";
    }

    /**
     * Module 2 – Account activated / deactivated notification email.
     */
    private String buildStatusEmailBody(String userName, String heading,
                                         String mainMessage, String subMessage,
                                         String accentColor, String accentBg) {
        return "<!DOCTYPE html>" +
               "<html lang=\"en\">" +
               "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"></head>" +
               "<body style=\"margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;\">" +
               "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f6f8;padding:40px 20px;\">" +
               "    <tr><td align=\"center\">" +
               "      <table width=\"520\" cellpadding=\"0\" cellspacing=\"0\"" +
               "             style=\"background:#ffffff;border-radius:12px;box-shadow:0 4px 20px rgba(0,0,0,0.08);overflow:hidden;max-width:520px;width:100%;\">" +
               "        <!-- Header -->" +
               "        <tr>" +
               "          <td style=\"background:" + accentColor + ";padding:24px 32px;\">" +
               "            <h1 style=\"color:#ffffff;margin:0;font-size:22px;font-weight:700;\">StockPro</h1>" +
               "            <p style=\"color:rgba(255,255,255,0.85);margin:4px 0 0;font-size:13px;\">Inventory Management System</p>" +
               "          </td>" +
               "        </tr>" +
               "        <!-- Body -->" +
               "        <tr>" +
               "          <td style=\"padding:32px 40px;\">" +
               "            <h2 style=\"color:#2c3e50;font-size:20px;margin-top:0;\">" + heading + "</h2>" +
               "            <p style=\"color:#555;font-size:15px;\">Hi <strong>" + userName + "</strong>,</p>" +
               "            <div style=\"background:" + accentBg + ";border-left:4px solid " + accentColor + ";" +
               "                        padding:14px 18px;border-radius:6px;margin:20px 0;\">" +
               "              <p style=\"color:#333;font-size:15px;margin:0;line-height:1.6;\">" + mainMessage + "</p>" +
               "            </div>" +
               "            <p style=\"color:#777;font-size:14px;line-height:1.6;\">" + subMessage + "</p>" +
               "          </td>" +
               "        </tr>" +
               "        <!-- Footer -->" +
               "        <tr>" +
               "          <td style=\"background:#f8fafc;border-top:1px solid #eee;padding:16px 32px;text-align:center;\">" +
               "            <p style=\"color:#bbb;font-size:11px;margin:0;\">" +
               "              &copy; 2024 StockPro &bull; Do not reply to this email" +
               "            </p>" +
               "          </td>" +
               "        </tr>" +
               "      </table>" +
               "    </td></tr>" +
               "  </table>" +
               "</body></html>";
    }

    // ─── Core Sender ─────────────────────────────────────────────────────────

    /**
     * Sends an HTML email using MimeMessage so email clients render the HTML.
     */
    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new EmailDeliveryException(
                    "Unable to send email. Please verify the mail configuration and try again.", ex);
        }
    }
}

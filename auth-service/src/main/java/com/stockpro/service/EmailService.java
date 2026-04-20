package com.stockpro.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public void sendOtpEmail(String toEmail, String otp, String subject, String purpose) {
        if (mailUsername == null || mailUsername.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            log.warn("SMTP not configured - skipping email send. OTP for {} is {}", toEmail, otp);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailUsername);
            helper.setTo(toEmail);
            helper.setReplyTo(mailUsername);
            helper.setSubject(subject != null && !subject.isBlank() ? subject : "Secure Verification Code");

            String messageText = switch (purpose) {
                case "REGISTER" -> "Use this OTP to complete your registration. This code is valid for 5 minutes.";
                case "FORGOT_PASSWORD" -> "Use this OTP to reset your password. This code is valid for 5 minutes.";
                default -> "Use this OTP for verification. This code is valid for 5 minutes.";
            };

            String htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>OTP Verification</title>
                    </head>
                    <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
                    
                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="background-color:#f4f6f8; margin:0; padding:20px 0;">
                            <tr>
                                <td align="center">
                                
                                    <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width:520px; background-color:#ffffff; border-radius:10px; overflow:hidden; border:1px solid #e5e7eb;">
                                        <tr>
                                            <td align="center" style="background-color:#1f3b57; padding:22px;">
                                                <h1 style="margin:0; color:#ffffff; font-size:24px; font-weight:700; letter-spacing:1px;">
                                                    SECURE AUTH
                                                </h1>
                                            </td>
                                        </tr>

                                        <tr>
                                            <td style="padding:35px 30px 20px 30px; text-align:center;">
                                                <p style="margin:0 0 12px 0; font-size:18px; color:#222222; font-weight:600;">
                                                    Hello,
                                                </p>

                                                <p style="margin:0 0 24px 0; font-size:16px; line-height:24px; color:#555555;">
                                                    %s
                                                </p>

                                                <p style="margin:0 0 12px 0; font-size:14px; color:#777777;">
                                                    Your One-Time Password
                                                </p>

                                                <div style="margin:0 0 24px 0;">
                                                    <span style="
                                                        display:inline-block;
                                                        padding:14px 24px;
                                                        font-size:32px;
                                                        line-height:36px;
                                                        font-weight:700;
                                                        color:#1d4ed8;
                                                        background-color:#eef4ff;
                                                        border:1px solid #c7d7fe;
                                                        border-radius:8px;
                                                        font-family:'Courier New', Courier, monospace;
                                                        letter-spacing:6px;
                                                        white-space:nowrap;
                                                    ">%s</span>
                                                </div>

                                                <p style="margin:0 0 10px 0; font-size:14px; color:#666666; line-height:22px;">
                                                    This OTP is valid for <strong>5 minutes</strong>.
                                                </p>

                                                <p style="margin:0; font-size:13px; color:#999999; line-height:20px;">
                                                    If you did not request this, please ignore this email.
                                                </p>
                                            </td>
                                        </tr>

                                        <tr>
                                            <td align="center" style="padding:18px; background-color:#fafafa; border-top:1px solid #eeeeee;">
                                                <p style="margin:0; font-size:12px; color:#aaaaaa;">
                                                    © 2026 Secure Auth Systems
                                                </p>
                                            </td>
                                        </tr>
                                    </table>

                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                    """
                    .formatted(messageText, otp);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("OTP email sent successfully to {}", toEmail);

        } catch (MessagingException | MailException e) {
            log.error("OTP email send failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
package com.stockpro.alert.mail;

import com.stockpro.alert.exception.EmailDispatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JavaMailService implements MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaMailService.class);

    private final JavaMailSender javaMailSender;

    @Value("${alert.email.from:}")
    private String fromAddress;

    public JavaMailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (StringUtils.hasText(fromAddress)) {
                message.setFrom(fromAddress);
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            javaMailSender.send(message);
            LOGGER.info("Sent alert email to {}", to);
        } catch (MailException exception) {
            LOGGER.error("Failed to send alert email to {}: {}", to, exception.getMessage(), exception);
            throw new EmailDispatchException("Failed to send alert email.", exception);
        }
    }
}

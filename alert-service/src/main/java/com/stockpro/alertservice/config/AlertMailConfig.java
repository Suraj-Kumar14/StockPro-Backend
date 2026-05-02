package com.stockpro.alertservice.config;

import com.stockpro.alertservice.mail.EmailNotificationService;
import com.stockpro.alertservice.mail.JavaMailEmailNotificationService;
import com.stockpro.alertservice.mail.NoOpEmailNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class AlertMailConfig {

    @Bean
    public EmailNotificationService emailNotificationService(
            JavaMailSender mailSender,
            @Value("${stockpro.alert.mail.enabled:false}") boolean enabled,
            @Value("${stockpro.alert.mail.from-address:noreply@stockpro.local}") String fromAddress,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.username:}") String username) {
        if (enabled && host != null && !host.isBlank() && username != null && !username.isBlank()) {
            return new JavaMailEmailNotificationService(mailSender, fromAddress);
        }
        return new NoOpEmailNotificationService();
    }
}

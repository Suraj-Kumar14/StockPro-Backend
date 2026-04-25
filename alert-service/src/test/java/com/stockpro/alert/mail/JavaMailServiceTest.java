package com.stockpro.alert.mail;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JavaMailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    private JavaMailService javaMailService;

    @BeforeEach
    void setUp() {
        javaMailService = new JavaMailService(javaMailSender);
        ReflectionTestUtils.setField(javaMailService, "fromAddress", "alerts@stockpro.com");
    }

    @Test
    void sendEmailShouldUseJavaMailSender() {
        javaMailService.sendEmail("recipient@stockpro.com", "Alert subject", "Alert body");

        verify(javaMailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }
}

package com.stockpro.alert.mail;

public interface MailService {

    void sendEmail(String to, String subject, String body);
}

package com.stockpro.paymentservice.persistence;

import com.stockpro.paymentservice.enums.PaymentMethod;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Converter(autoApply = false)
@Slf4j
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethod attribute) {
        return attribute == null ? PaymentMethod.RAZORPAY.name() : attribute.name();
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return PaymentMethod.RAZORPAY;
        }

        try {
            return PaymentMethod.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown payment_method '{}' found in database. Falling back to RAZORPAY.", dbData);
            return PaymentMethod.RAZORPAY;
        }
    }
}

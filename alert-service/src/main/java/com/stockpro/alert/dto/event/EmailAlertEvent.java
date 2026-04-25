package com.stockpro.alert.dto.event;

import com.stockpro.alert.enums.AlertSeverity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAlertEvent {

    @NotNull(message = "recipientId is required.")
    @Positive(message = "recipientId must be greater than zero.")
    private Long recipientId;

    @NotBlank(message = "toEmail is required.")
    @Email(message = "toEmail must be a valid email address.")
    private String toEmail;

    @NotBlank(message = "subject is required.")
    @Size(max = 255, message = "subject cannot be longer than 255 characters.")
    private String subject;

    @NotBlank(message = "body is required.")
    @Size(max = 4000, message = "body cannot be longer than 4000 characters.")
    private String body;

    @NotNull(message = "alertId is required.")
    @Positive(message = "alertId must be greater than zero.")
    private Long alertId;

    @NotNull(message = "severity is required.")
    private AlertSeverity severity;
}

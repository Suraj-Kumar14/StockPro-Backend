package com.stockpro.alert.dto.event;

import com.stockpro.alert.enums.AlertChannel;
import com.stockpro.alert.enums.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
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
public class SystemAlertEvent {

    @NotNull(message = "recipientId is required.")
    @Positive(message = "recipientId must be greater than zero.")
    private Long recipientId;

    @NotBlank(message = "title is required.")
    @Size(max = 255, message = "title cannot be longer than 255 characters.")
    private String title;

    @NotBlank(message = "message is required.")
    @Size(max = 1000, message = "message cannot be longer than 1000 characters.")
    private String message;

    @NotNull(message = "severity is required.")
    private AlertSeverity severity;

    private AlertChannel channel;

    @NotNull(message = "eventTime is required.")
    private LocalDateTime eventTime;
}

package com.stockpro.alertservice.dto.request;

import com.stockpro.alertservice.enums.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class CreateBroadcastAlertRequest {
    private List<String> recipientRoles;
    private List<Long> recipientIds;
    @NotNull
    private AlertSeverity severity;
    @NotBlank
    private String title;
    @NotBlank
    private String message;
    private LocalDateTime expiresAt;
    private String actionUrl;
}

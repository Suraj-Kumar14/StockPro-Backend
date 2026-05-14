package com.stockpro.alertservice.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.stockpro.alertservice.enums.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CreateBroadcastAlertRequest {
    @JsonAlias({"targetRoles"})
    private List<String> recipientRoles;
    @JsonAlias({"targetUsers", "userIds"})
    private List<Long> recipientIds;
    @JsonAlias({"targetRole"})
    private String targetRole;
    @NotNull
    private AlertSeverity severity;
    @NotBlank
    private String title;
    @NotBlank
    private String message;
    private LocalDateTime expiresAt;
    private String actionUrl;

    public List<String> resolveRecipientRoles() {
        List<String> roles = new ArrayList<>();
        if (recipientRoles != null) {
            roles.addAll(recipientRoles);
        }
        if (targetRole != null && !targetRole.isBlank()) {
            roles.add(targetRole);
        }
        return roles;
    }
}

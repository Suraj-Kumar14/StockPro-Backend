package com.stockpro.web.service.impl;

import com.stockpro.web.client.AlertServiceClient;
import com.stockpro.web.dto.response.AlertResponse;
import com.stockpro.web.dto.response.UnreadCountResponse;
import com.stockpro.web.service.UiContextService;
import com.stockpro.web.util.SecurityUtils;
import com.stockpro.web.viewmodel.HeaderAlertViewModel;
import com.stockpro.web.viewmodel.HeaderViewModel;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UiContextServiceImpl implements UiContextService {

    private static final DateTimeFormatter ALERT_TIME = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    private final AlertServiceClient alertServiceClient;

    public UiContextServiceImpl(AlertServiceClient alertServiceClient) {
        this.alertServiceClient = alertServiceClient;
    }

    @Override
    public HeaderViewModel buildHeader() {
        return SecurityUtils.getCurrentUser()
                .map(user -> {
                    UnreadCountResponse unreadCountResponse = alertServiceClient.getUnreadCount(user.getUserId());
                    List<HeaderAlertViewModel> previewAlerts = alertServiceClient.getAlertsByRecipient(user.getUserId()).stream()
                            .sorted(Comparator.comparing(AlertResponse::getCreatedAt,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                            .limit(5)
                            .map(alert -> new HeaderAlertViewModel(
                                    alert.getAlertId(),
                                    alert.getTitle(),
                                    alert.getMessage(),
                                    alert.getSeverity() == null ? "INFO" : alert.getSeverity().name(),
                                    alert.getCreatedAt() == null ? "-" : alert.getCreatedAt().format(ALERT_TIME),
                                    !Boolean.TRUE.equals(alert.getRead())))
                            .toList();

                    return new HeaderViewModel(
                            user.getFullName(),
                            user.getEmail(),
                            user.getRole(),
                            user.getUserId(),
                            unreadCountResponse == null ? 0 : unreadCountResponse.getUnreadCount(),
                            previewAlerts);
                })
                .orElseGet(() -> new HeaderViewModel("Guest", "", "", null, 0, List.of()));
    }
}

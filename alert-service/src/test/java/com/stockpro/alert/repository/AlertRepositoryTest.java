package com.stockpro.alert.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.enums.AlertChannel;
import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
    }

    @Test
    void countUnreadByRecipientShouldReturnExpectedValue() {
        alertRepository.saveAll(List.of(
                alert(101L, AlertType.LOW_STOCK, AlertSeverity.WARNING, Boolean.FALSE, Boolean.FALSE, LocalDateTime.of(2026, 4, 25, 9, 0)),
                alert(101L, AlertType.SYSTEM, AlertSeverity.INFO, Boolean.FALSE, Boolean.FALSE, LocalDateTime.of(2026, 4, 25, 10, 0)),
                alert(101L, AlertType.PO_PENDING, AlertSeverity.INFO, Boolean.TRUE, Boolean.FALSE, LocalDateTime.of(2026, 4, 25, 11, 0))));

        long unreadCount = alertRepository.countByRecipientIdAndIsRead(101L, Boolean.FALSE);

        assertThat(unreadCount).isEqualTo(2L);
    }

    @Test
    void findByRecipientIdAndIsReadOrderByCreatedAtDescShouldSortNewestFirst() {
        alertRepository.saveAll(List.of(
                alert(101L, AlertType.SYSTEM, AlertSeverity.INFO, Boolean.FALSE, Boolean.FALSE, LocalDateTime.of(2026, 4, 25, 8, 0)),
                alert(101L, AlertType.LOW_STOCK, AlertSeverity.CRITICAL, Boolean.FALSE, Boolean.FALSE, LocalDateTime.of(2026, 4, 25, 9, 30)),
                alert(101L, AlertType.PO_PENDING, AlertSeverity.WARNING, Boolean.TRUE, Boolean.FALSE, LocalDateTime.of(2026, 4, 25, 7, 30))));

        List<Alert> alerts = alertRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(101L, Boolean.FALSE);

        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.LOW_STOCK);
        assertThat(alerts.get(1).getType()).isEqualTo(AlertType.SYSTEM);
    }

    @Test
    void findByRecipientIdAndIsAcknowledgedOrderByCreatedAtDescShouldReturnUnacknowledgedAlerts() {
        alertRepository.saveAll(List.of(
                alert(202L, AlertType.OVERSTOCK, AlertSeverity.WARNING, Boolean.FALSE, Boolean.FALSE, LocalDateTime.of(2026, 4, 25, 12, 0)),
                alert(202L, AlertType.SYSTEM, AlertSeverity.INFO, Boolean.FALSE, Boolean.TRUE, LocalDateTime.of(2026, 4, 25, 12, 30)),
                alert(202L, AlertType.OVERDUE_RECEIPT, AlertSeverity.CRITICAL, Boolean.FALSE, Boolean.FALSE, LocalDateTime.of(2026, 4, 25, 13, 0))));

        List<Alert> alerts = alertRepository.findByRecipientIdAndIsAcknowledgedOrderByCreatedAtDesc(202L, Boolean.FALSE);

        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).getType()).isEqualTo(AlertType.OVERDUE_RECEIPT);
        assertThat(alerts.get(1).getType()).isEqualTo(AlertType.OVERSTOCK);
    }

    private Alert alert(Long recipientId,
            AlertType type,
            AlertSeverity severity,
            Boolean isRead,
            Boolean isAcknowledged,
            LocalDateTime createdAt) {
        return Alert.builder()
                .recipientId(recipientId)
                .type(type)
                .severity(severity)
                .title(type.name() + " alert")
                .message("Alert for " + type.name())
                .channel(AlertChannel.IN_APP)
                .isRead(isRead)
                .isAcknowledged(isAcknowledged)
                .createdAt(createdAt)
                .build();
    }
}

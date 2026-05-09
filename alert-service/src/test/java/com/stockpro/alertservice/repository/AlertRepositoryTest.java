package com.stockpro.alertservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.enums.AlertChannel;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AlertRepositoryTest {

    @Autowired
    private AlertRepository alertRepository;

    private Alert managerAlert;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        managerAlert = alertRepository.save(alert(1L, "MANAGER", "ALT-001", false, LocalDateTime.now().plusDays(1), "corr-1"));
        alertRepository.save(alert(2L, "STAFF", "ALT-002", true, LocalDateTime.now().minusDays(1), "corr-2"));
        alertRepository.save(alert(null, "MANAGER", "ALT-003", false, LocalDateTime.now().plusDays(2), "corr-3"));
    }

    @Test
    void shouldFindAlertsByRecipientAndUnreadFlags() {
        assertEquals(1, alertRepository.findByRecipientId(1L, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(2, alertRepository.findByRecipientRole("MANAGER", PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, alertRepository.findByRecipientIdAndIsReadFalse(1L, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1L, alertRepository.countByRecipientIdAndIsReadFalse(1L));
        assertEquals(2L, alertRepository.countByRecipientRoleAndIsReadFalse("MANAGER"));
    }

    @Test
    void shouldSupportLookupCountsAndExpirationQueries() {
        assertTrue(alertRepository.findByAlertId(managerAlert.getAlertId()).isPresent());
        assertTrue(alertRepository.findByAlertNumber("ALT-001").isPresent());
        assertTrue(alertRepository.existsByAlertNumber("ALT-003"));
        assertTrue(alertRepository.existsByCorrelationIdAndTypeAndRecipientId("corr-1", AlertType.LOW_STOCK, 1L));
        assertTrue(alertRepository.existsByCorrelationIdAndTypeAndRecipientRole("corr-2", AlertType.LOW_STOCK, "STAFF"));
        assertEquals(1, alertRepository.findByExpiresAtBeforeAndStatusNot(LocalDateTime.now(), AlertStatus.DISMISSED).size());
        assertEquals(3L, alertRepository.countByType(AlertType.LOW_STOCK));
        assertEquals(3L, alertRepository.countBySeverity(AlertSeverity.WARNING));
        assertEquals("ALT-003", alertRepository.findTopByAlertNumberStartingWithOrderByAlertNumberDesc("ALT-").orElseThrow().getAlertNumber());
    }

    private Alert alert(Long recipientId, String role, String number, boolean read, LocalDateTime expiresAt, String correlationId) {
        return Alert.builder()
                .alertNumber(number)
                .recipientId(recipientId)
                .recipientRole(role)
                .type(AlertType.LOW_STOCK)
                .severity(AlertSeverity.WARNING)
                .status(AlertStatus.NEW)
                .channel(AlertChannel.IN_APP)
                .title("Alert")
                .message("Message")
                .isRead(read)
                .isAcknowledged(false)
                .isDismissed(false)
                .expiresAt(expiresAt)
                .correlationId(correlationId)
                .build();
    }
}

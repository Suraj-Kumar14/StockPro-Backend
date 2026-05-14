package com.stockpro.alertservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockpro.alertservice.dto.request.AlertSearchRequest;
import com.stockpro.alertservice.dto.request.CreateAlertRequest;
import com.stockpro.alertservice.dto.response.AlertAnalyticsResponse;
import com.stockpro.alertservice.dto.response.AlertResponse;
import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.enums.AlertChannel;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import com.stockpro.alertservice.mail.EmailNotificationService;
import com.stockpro.alertservice.rabbitmq.AlertEventPublisher;
import com.stockpro.alertservice.repository.AlertRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import({AlertService.class, AlertValidationService.class, AlertMapper.class})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = "stockpro.alert.overdue-critical-days=3")
class AlertServiceCoverageTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertRepository alertRepository;

    @MockBean
    private AlertEventPublisher alertEventPublisher;

    @MockBean
    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
    }

    @Test
    void searchAlerts_shouldExecuteSpecificationFilters() {
        alertRepository.save(alertBuilder()
                .alertNumber("ALT-20260514-000001")
                .recipientId(7L)
                .recipientRole("INVENTORY_MANAGER")
                .type(AlertType.LOW_STOCK)
                .severity(AlertSeverity.WARNING)
                .status(AlertStatus.NEW)
                .message("Low stock warning for widget")
                .referenceType("PRODUCT")
                .referenceId("9")
                .referenceNumber("REF-9")
                .sourceService("warehouse-service")
                .createdAt(LocalDateTime.of(2026, 5, 14, 10, 0))
                .build());

        alertRepository.save(alertBuilder()
                .alertNumber("ALT-20260514-000002")
                .recipientId(99L)
                .recipientRole("ADMIN")
                .type(AlertType.OVERSTOCK)
                .severity(AlertSeverity.INFO)
                .status(AlertStatus.READ)
                .message("Other alert")
                .referenceType("WAREHOUSE")
                .referenceId("4")
                .referenceNumber("REF-4")
                .sourceService("alert-service")
                .createdAt(LocalDateTime.of(2026, 5, 10, 10, 0))
                .build());

        AlertSearchRequest searchRequest = AlertSearchRequest.builder()
                .keyword("widget")
                .recipientId(7L)
                .recipientRole("MANAGER")
                .type(AlertType.LOW_STOCK)
                .severity(AlertSeverity.WARNING)
                .status(AlertStatus.NEW)
                .isRead(false)
                .isAcknowledged(false)
                .isDismissed(false)
                .referenceType("PRODUCT")
                .referenceId("9")
                .sourceService("warehouse-service")
                .fromDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                .toDate(LocalDateTime.of(2026, 5, 14, 23, 59))
                .page(0)
                .size(10)
                .sortBy("createdAt")
                .sortDir("desc")
                .build();

        Page<AlertResponse> page = alertService.searchAlerts(searchRequest, 7L, "ADMIN", true);

        assertEquals(1, page.getTotalElements());
        assertEquals("ALT-20260514-000001", page.getContent().get(0).getAlertNumber());
    }

    @Test
    void myAlertsUnreadCountAndAnalytics_shouldUseJpaSpecifications() {
        alertRepository.save(alertBuilder()
                .alertNumber("ALT-20260514-000003")
                .recipientId(null)
                .recipientRole("STAFF")
                .type(AlertType.LOW_STOCK)
                .severity(AlertSeverity.CRITICAL)
                .status(AlertStatus.NEW)
                .message("Legacy role alert")
                .relatedProductId(44L)
                .relatedWarehouseId(8L)
                .createdAt(LocalDateTime.of(2026, 5, 14, 8, 0))
                .build());

        alertRepository.save(alertBuilder()
                .alertNumber("ALT-20260514-000004")
                .recipientId(33L)
                .recipientRole("WAREHOUSE_STAFF")
                .type(AlertType.OVERSTOCK)
                .severity(AlertSeverity.WARNING)
                .status(AlertStatus.READ)
                .message("Read alert")
                .isRead(true)
                .relatedProductId(44L)
                .relatedWarehouseId(8L)
                .createdAt(LocalDateTime.of(2026, 5, 13, 8, 0))
                .build());

        long unreadCount = alertService.getUnreadCount(33L, "WAREHOUSE_STAFF");
        Page<AlertResponse> myAlerts = alertService.getMyAlerts(
                33L,
                "WAREHOUSE_STAFF",
                AlertSearchRequest.builder().page(0).size(10).sortBy("createdAt").sortDir("desc").build());
        AlertAnalyticsResponse analytics = alertService.getAlertAnalytics(
                LocalDateTime.of(2026, 5, 13, 0, 0),
                LocalDateTime.of(2026, 5, 14, 23, 59));

        assertEquals(1L, unreadCount);
        assertEquals(2, myAlerts.getTotalElements());
        assertEquals(1L, analytics.getAlertsBySeverity().get("WARNING"));
        assertEquals(1L, analytics.getAlertsBySeverity().get("CRITICAL"));
        assertFalse(analytics.getTopAlertedProducts().isEmpty());
    }

    @Test
    void createAlert_shouldSanitizeSqlErrorMessages() {
        CreateAlertRequest createRequest = new CreateAlertRequest();
        createRequest.setRecipientRole("ADMIN");
        createRequest.setType(AlertType.SYSTEM_ERROR);
        createRequest.setSeverity(AlertSeverity.CRITICAL);
        createRequest.setChannel(AlertChannel.IN_APP);
        createRequest.setTitle("Database failure");
        createRequest.setMessage("SQL grammar exception while executing update statement");
        createRequest.setTechnicalDetails("jdbc sql stack trace with exception details");

        AlertResponse response = alertService.createAlert(createRequest, 91L);

        assertEquals("A system operation failed. Please try again or contact admin.", response.getMessage());
        Alert saved = alertRepository.findByAlertNumber(response.getAlertNumber()).orElseThrow();
        assertEquals("System error details were captured in backend logs for further investigation.",
                saved.getTechnicalDetails());
    }

    @Test
    void createAlert_shouldTruncateLongSystemErrorContent() {
        String longMessage = "safe-message-".repeat(220);
        String longTechnicalDetails = "details-".repeat(320);

        CreateAlertRequest createRequest = new CreateAlertRequest();
        createRequest.setRecipientRole("ADMIN");
        createRequest.setType(AlertType.SYSTEM_ERROR);
        createRequest.setSeverity(AlertSeverity.CRITICAL);
        createRequest.setChannel(AlertChannel.IN_APP);
        createRequest.setTitle("Long system error");
        createRequest.setMessage(longMessage);
        createRequest.setUserMessage(longMessage);
        createRequest.setTechnicalDetails(longTechnicalDetails);

        AlertResponse response = alertService.createAlert(createRequest, 92L);
        Alert saved = alertRepository.findByAlertNumber(response.getAlertNumber()).orElseThrow();

        assertNotNull(saved.getTechnicalDetails());
        assertTrue(saved.getMessage().length() <= 2000);
        assertTrue(saved.getTechnicalDetails().length() <= 2000);
        assertTrue(saved.getMessage().endsWith("..."));
        assertTrue(saved.getTechnicalDetails().endsWith("..."));
    }

    private Alert.AlertBuilder alertBuilder() {
        return Alert.builder()
                .channel(AlertChannel.IN_APP)
                .title("Alert")
                .message("Message")
                .userMessage("Message")
                .isRead(false)
                .isAcknowledged(false)
                .isDismissed(false)
                .isArchived(false)
                .createdAt(LocalDateTime.now());
    }
}

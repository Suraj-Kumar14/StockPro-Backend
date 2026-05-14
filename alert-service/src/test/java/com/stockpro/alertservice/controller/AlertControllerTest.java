package com.stockpro.alertservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.alertservice.dto.request.CreateAlertRequest;
import com.stockpro.alertservice.dto.request.CreateBroadcastAlertRequest;
import com.stockpro.alertservice.dto.response.AlertAnalyticsResponse;
import com.stockpro.alertservice.dto.response.AlertResponse;
import com.stockpro.alertservice.dto.response.AlertSummaryResponse;
import com.stockpro.alertservice.enums.AlertChannel;
import com.stockpro.alertservice.enums.AlertSeverity;
import com.stockpro.alertservice.enums.AlertStatus;
import com.stockpro.alertservice.enums.AlertType;
import com.stockpro.alertservice.exception.GlobalExceptionHandler;
import com.stockpro.alertservice.security.AuthenticatedUser;
import com.stockpro.alertservice.service.AlertService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlertController.class)
@Import({AlertControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private AlertService alertService;

    @Test
    void createAlertAndBroadcast_shouldReturnCreatedForAdmin() throws Exception {
        when(alertService.createAlert(any(CreateAlertRequest.class), eq(10L))).thenReturn(alertResponse());
        when(alertService.createBroadcastAlert(any(CreateBroadcastAlertRequest.class), eq(10L))).thenReturn(List.of(alertResponse()));

        mockMvc.perform(post("/api/v1/alerts")
                        .with(authentication(auth(10L, "ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAlertRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertNumber").value("ALT-001"));

        mockMvc.perform(post("/api/v1/alerts/broadcast")
                        .with(authentication(auth(10L, "ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(broadcastRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].title").value("Alert title"));
    }

    @Test
    void createAlert_shouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/alerts")
                        .with(authentication(auth(10L, "ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void createBroadcast_shouldRejectInvalidSeverityValue() throws Exception {
        mockMvc.perform(post("/api/v1/alerts/broadcast")
                        .with(authentication(auth(10L, "ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRole": "ALL",
                                  "severity": "INVALID",
                                  "title": "Alert title",
                                  "message": "Alert message"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void myAlertsSearchAndSummaryEndpoints_shouldReturnResponseBodies() throws Exception {
        when(alertService.getMyAlerts(eq(11L), eq("MANAGER"), any()))
                .thenReturn(new PageImpl<>(List.of(alertResponse())));
        when(alertService.searchAlerts(any(), eq(11L), eq("MANAGER"), eq(false)))
                .thenReturn(new PageImpl<>(List.of(alertResponse())));
        when(alertService.getUnreadCount(11L, "MANAGER")).thenReturn(3L);
        when(alertService.getMyAlertSummary(11L, "MANAGER")).thenReturn(AlertSummaryResponse.builder()
                .totalAlerts(5).unreadCount(3).criticalCount(1).build());
        when(alertService.getAlertAnalytics(any(), any())).thenReturn(AlertAnalyticsResponse.builder()
                .alertsByType(Map.of("LOW_STOCK", 2L))
                .alertsBySeverity(Map.of("CRITICAL", 1L))
                .build());

        mockMvc.perform(get("/api/v1/alerts/my").with(authentication(auth(11L, "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Alert title"));

        mockMvc.perform(get("/api/v1/alerts/search").with(authentication(auth(11L, "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].alertId").value(1L));

        mockMvc.perform(get("/api/v1/alerts/unread-count").with(authentication(auth(11L, "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));

        mockMvc.perform(get("/api/v1/alerts/summary/my").with(authentication(auth(11L, "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlerts").value(5));

        mockMvc.perform(get("/api/v1/alerts/analytics")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-09")
                        .with(authentication(auth(11L, "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertsByType.LOW_STOCK").value(2));
    }

    @Test
    void alertMutationEndpoints_shouldForwardActorDetails() throws Exception {
        when(alertService.markAsRead(1L, 15L, "OFFICER", false)).thenReturn(alertResponse());
        when(alertService.acknowledgeAlert(eq(1L), any(), eq(15L), eq("OFFICER"), eq(false))).thenReturn(alertResponse());
        when(alertService.dismissAlert(eq(1L), any(), eq(15L), eq("OFFICER"), eq(false))).thenReturn(alertResponse());
        when(alertService.resolveAlert(1L, 99L)).thenReturn(alertResponse());

        mockMvc.perform(patch("/api/v1/alerts/1/read").with(authentication(auth(15L, "OFFICER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"));

        mockMvc.perform(post("/api/v1/alerts/read-all").with(authentication(auth(15L, "OFFICER"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/v1/alerts/1/acknowledge")
                        .with(authentication(auth(15L, "OFFICER")))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/alerts/1/dismiss")
                        .with(authentication(auth(15L, "OFFICER")))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/alerts/1/resolve")
                        .with(authentication(auth(99L, "ADMIN"))))
                .andExpect(status().isOk());

        verify(alertService).markAllAsRead(15L, "OFFICER");
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }

    private UsernamePasswordAuthenticationToken auth(Long userId, String role) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "user@example.com", role, "token"),
                "token",
                AuthorityUtils.createAuthorityList("ROLE_" + role));
    }

    private CreateAlertRequest createAlertRequest() {
        CreateAlertRequest request = new CreateAlertRequest();
        request.setRecipientRole("MANAGER");
        request.setType(AlertType.LOW_STOCK);
        request.setSeverity(AlertSeverity.WARNING);
        request.setChannel(AlertChannel.IN_APP);
        request.setTitle("Alert title");
        request.setMessage("Alert message");
        return request;
    }

    private CreateBroadcastAlertRequest broadcastRequest() {
        CreateBroadcastAlertRequest request = new CreateBroadcastAlertRequest();
        request.setRecipientRoles(List.of("MANAGER", "ADMIN"));
        request.setSeverity(AlertSeverity.INFO);
        request.setTitle("Alert title");
        request.setMessage("Alert message");
        return request;
    }

    private AlertResponse alertResponse() {
        return AlertResponse.builder()
                .alertId(1L)
                .alertNumber("ALT-001")
                .recipientRole("MANAGER")
                .type(AlertType.LOW_STOCK)
                .severity(AlertSeverity.WARNING)
                .status(AlertStatus.NEW)
                .channel(AlertChannel.IN_APP)
                .title("Alert title")
                .message("Alert message")
                .isRead(false)
                .isAcknowledged(false)
                .isDismissed(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}

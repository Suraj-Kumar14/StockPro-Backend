package com.stockpro.alert.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.alert.config.JwtAuthenticationFilter;
import com.stockpro.alert.dto.request.SendAlertRequest;
import com.stockpro.alert.dto.response.AlertResponse;
import com.stockpro.alert.dto.response.UnreadCountResponse;
import com.stockpro.alert.enums.AlertChannel;
import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
import com.stockpro.alert.service.AlertService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlertController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void sendAlertShouldReturnCreatedResponse() throws Exception {
        SendAlertRequest request = SendAlertRequest.builder()
                .recipientId(101L)
                .type(AlertType.SYSTEM)
                .severity(AlertSeverity.INFO)
                .title("System notice")
                .message("Inventory dashboard update.")
                .channel(AlertChannel.IN_APP)
                .build();

        when(alertService.sendAlert(any(SendAlertRequest.class))).thenReturn(alertResponse(1L, 101L, AlertType.SYSTEM));

        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertId").value(1))
                .andExpect(jsonPath("$.recipientId").value(101))
                .andExpect(jsonPath("$.type").value("SYSTEM"));
    }

    @Test
    void getUnreadCountShouldReturnPayload() throws Exception {
        when(alertService.getUnreadCount(101L)).thenReturn(UnreadCountResponse.builder()
                .recipientId(101L)
                .unreadCount(5L)
                .build());

        mockMvc.perform(get("/api/v1/alerts/unread-count/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientId").value(101))
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void getAlertsShouldReturnPagedContent() throws Exception {
        when(alertService.searchAlerts(any())).thenReturn(new PageImpl<>(List.of(alertResponse(1L, 101L, AlertType.LOW_STOCK))));

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].alertId").value(1))
                .andExpect(jsonPath("$.content[0].type").value("LOW_STOCK"));
    }

    private AlertResponse alertResponse(Long alertId, Long recipientId, AlertType type) {
        return AlertResponse.builder()
                .alertId(alertId)
                .recipientId(recipientId)
                .type(type)
                .severity(AlertSeverity.WARNING)
                .title("Alert title")
                .message("Alert message")
                .channel(AlertChannel.IN_APP)
                .isRead(Boolean.FALSE)
                .isAcknowledged(Boolean.FALSE)
                .createdAt(LocalDateTime.of(2026, 4, 25, 10, 0))
                .build();
    }
}

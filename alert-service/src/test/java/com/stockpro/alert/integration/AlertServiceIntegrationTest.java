package com.stockpro.alert.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.alert.client.AuthServiceClient;
import com.stockpro.alert.config.JwtAuthenticationFilter;
import com.stockpro.alert.dto.response.AuthUserResponse;
import com.stockpro.alert.enums.AlertChannel;
import com.stockpro.alert.enums.AlertSeverity;
import com.stockpro.alert.enums.AlertType;
import com.stockpro.alert.publisher.AlertEventPublisher;
import com.stockpro.alert.repository.AlertRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AlertServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AlertRepository alertRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private AuthServiceClient authServiceClient;

    @MockBean
    private AlertEventPublisher alertEventPublisher;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        when(authServiceClient.getAllUsers()).thenReturn(List.of(
                AuthUserResponse.builder().userId(101L).email("recipient101@stockpro.com").build()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAlertShouldPersistAlertAndPublishEmailEventForCriticalSeverity() throws Exception {
        var request = java.util.Map.of(
                "recipientId", 101,
                "type", AlertType.SYSTEM.name(),
                "severity", AlertSeverity.CRITICAL.name(),
                "title", "Critical incident",
                "message", "Stock movement processing is delayed.",
                "channel", AlertChannel.IN_APP.name());

        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertId").isNumber())
                .andExpect(jsonPath("$.channel").value("BOTH"))
                .andExpect(jsonPath("$.severity").value("CRITICAL"));

        org.assertj.core.api.Assertions.assertThat(alertRepository.count()).isEqualTo(1L);
        verify(alertEventPublisher).publishEmailAlert(any());
    }
}

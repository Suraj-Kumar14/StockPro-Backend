package com.stockpro.purchaseservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.purchaseservice.dto.response.PurchaseOrderResponse;
import com.stockpro.purchaseservice.dto.response.PurchaseAnalyticsResponse;
import com.stockpro.purchaseservice.dto.response.PurchaseOrderHistoryResponse;
import com.stockpro.purchaseservice.dto.response.PurchaseOrderReportRowResponse;
import com.stockpro.purchaseservice.dto.response.PurchaseOrderSummaryResponse;
import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.exception.InvalidPOStateException;
import com.stockpro.purchaseservice.publisher.SystemAlertPublisher;
import com.stockpro.purchaseservice.security.AuthenticatedUser;
import com.stockpro.purchaseservice.service.PurchaseOrderManagementService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PurchaseOrderApiV1Controller.class)
@Import(PurchaseOrderApiV1ControllerTest.TestSecurityConfig.class)
class PurchaseOrderApiV1ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseOrderManagementService purchaseOrderManagementService;

    @MockBean
    private SystemAlertPublisher systemAlertPublisher;

    @Test
    void createReturnsCreatedAndPassesAuthenticatedActorId() throws Exception {
        PurchaseOrderResponse response = sampleResponse();
        when(purchaseOrderManagementService.createPurchaseOrder(any(), eq(77L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .with(authentication(authenticatedUser(77L, "OFFICER")))
                        .contentType(APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purchaseOrderId").value(10L))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.supplierName").value("Acme Supplies"));

        verify(purchaseOrderManagementService).createPurchaseOrder(any(), eq(77L));
    }

    @Test
    void createReturnsForbiddenForUnauthorizedRole() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders")
                        .with(authentication(authenticatedUser(11L, "STAFF")))
                        .contentType(APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReturnsBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders")
                        .with(authentication(authenticatedUser(77L, "ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.supplierId").exists())
                .andExpect(jsonPath("$.warehouseId").exists())
                .andExpect(jsonPath("$.expectedDeliveryDate").exists())
                .andExpect(jsonPath("$.lineItems").exists());
    }

    @Test
    void searchReturnsBadRequestForInvalidStatusEnum() throws Exception {
        mockMvc.perform(get("/api/v1/purchase-orders/search")
                        .with(authentication(authenticatedUser(77L, "MANAGER")))
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for status"));
    }

    @Test
    void rejectReturnsBadRequestWhenServiceRaisesBusinessValidation() throws Exception {
        when(purchaseOrderManagementService.rejectPurchaseOrder(eq(10L), any(), eq(77L)))
                .thenThrow(new InvalidPOStateException("Purchase order cannot be rejected"));

        mockMvc.perform(post("/api/v1/purchase-orders/10/reject")
                        .with(authentication(authenticatedUser(77L, "MANAGER")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Missing vendor approval\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Purchase order cannot be rejected"));
    }

    @Test
    void purchaseOfficerSummaryReturnsServiceResponse() throws Exception {
        when(purchaseOrderManagementService.getPurchaseOfficerSummary(77L))
                .thenReturn(new PurchaseOrderSummaryResponse(
                        3, 1, 1, 1, 0, 0, 0, 0, 0,
                        BigDecimal.valueOf(2500), BigDecimal.valueOf(800), BigDecimal.ZERO));

        mockMvc.perform(get("/api/v1/purchase-orders/purchase-officer/summary")
                        .with(authentication(authenticatedUser(77L, "OFFICER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPurchaseOrders").value(3))
                .andExpect(jsonPath("$.pendingPurchaseValue").value(800));
    }

    @Test
    void getAllReturnsPagedResponseForAuthenticatedRole() throws Exception {
        when(purchaseOrderManagementService.getAllPurchaseOrders(0, 10, "createdAt", "desc"))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .with(authentication(authenticatedUser(77L, "STAFF"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].poNumber").value("PO-20260509-0001"));
    }

    @Test
    void readEndpointsReturnExpectedPayloads() throws Exception {
        when(purchaseOrderManagementService.searchPurchaseOrders(any(), any(), any(), any(), any(), any(), any(), any(), eq(0), eq(10), eq("createdAt"), eq("desc")))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));
        when(purchaseOrderManagementService.getPurchaseOrderById(10L)).thenReturn(sampleResponse());
        when(purchaseOrderManagementService.getPurchaseOrderByNumber("PO-20260509-0001")).thenReturn(sampleResponse());
        when(purchaseOrderManagementService.getPurchaseOrdersByStatus(POStatus.DRAFT)).thenReturn(List.of(sampleResponse()));
        when(purchaseOrderManagementService.getPurchaseOrderHistory(10L)).thenReturn(List.of(
                new PurchaseOrderHistoryResponse(1L, "CREATED", null, "DRAFT", 77L, "created", LocalDateTime.now())));
        when(purchaseOrderManagementService.getPurchaseOrderSummary()).thenReturn(new PurchaseOrderSummaryResponse(
                3, 1, 1, 1, 0, 0, 0, 0, 0, BigDecimal.valueOf(300), BigDecimal.valueOf(100), BigDecimal.ZERO));
        when(purchaseOrderManagementService.getPurchaseOrderReports(any(), any(), any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(new PurchaseOrderReportRowResponse(
                        10L, "PO-20260509-0001", "APPROVED", "PAID", "PAY-10", "order-1", "payment-1",
                        BigDecimal.valueOf(100), LocalDateTime.now(), 1L, "Acme Supplies", 2L, "Central Warehouse",
                        100L, "SKU-100", "Widget", null, BigDecimal.valueOf(25), 4, 4, 0, BigDecimal.valueOf(100),
                        BigDecimal.valueOf(100), LocalDate.now(), LocalDate.now().plusDays(5), 77L, LocalDateTime.now(), LocalDateTime.now()))));
        when(purchaseOrderManagementService.getPurchaseAnalytics(any(), any())).thenReturn(new PurchaseAnalyticsResponse(
                BigDecimal.valueOf(500), BigDecimal.valueOf(250), List.of("Supplier #1"), List.of("Product #100"), 1, 0, 12, 2));
        when(purchaseOrderManagementService.getOverduePurchaseOrders()).thenReturn(List.of(sampleResponse()));
        when(purchaseOrderManagementService.getPendingApprovalPurchaseOrders()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/purchase-orders/search")
                        .with(authentication(authenticatedUser(77L, "STAFF"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/purchase-orders/10")
                        .with(authentication(authenticatedUser(77L, "STAFF"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/purchase-orders/number/PO-20260509-0001")
                        .with(authentication(authenticatedUser(77L, "STAFF"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/purchase-orders/status/DRAFT")
                        .with(authentication(authenticatedUser(77L, "STAFF"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/purchase-orders/10/history")
                        .with(authentication(authenticatedUser(77L, "STAFF"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CREATED"));
        mockMvc.perform(get("/api/v1/purchase-orders/summary")
                        .with(authentication(authenticatedUser(77L, "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPurchaseOrders").value(3));
        mockMvc.perform(get("/api/v1/purchase-orders/reports")
                        .with(authentication(authenticatedUser(77L, "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentStatus").value("PAID"));
        mockMvc.perform(get("/api/v1/purchase-orders/analytics")
                        .with(authentication(authenticatedUser(77L, "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageApprovalTime").value(12));
        mockMvc.perform(get("/api/v1/purchase-orders/overdue")
                        .with(authentication(authenticatedUser(77L, "OFFICER"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/purchase-orders/pending-approval")
                        .with(authentication(authenticatedUser(77L, "MANAGER"))))
                .andExpect(status().isOk());
    }

    @Test
    void mutatingEndpointsDelegateAndReturnOk() throws Exception {
        when(purchaseOrderManagementService.updatePurchaseOrder(eq(10L), any(), eq(77L))).thenReturn(sampleResponse());
        when(purchaseOrderManagementService.submitPurchaseOrder(eq(10L), any(), eq(77L))).thenReturn(sampleResponse());
        when(purchaseOrderManagementService.submitForPayment(10L, 77L)).thenReturn(sampleResponse());
        when(purchaseOrderManagementService.approvePurchaseOrder(eq(10L), any(), eq(77L))).thenReturn(sampleResponse());
        when(purchaseOrderManagementService.markPaymentInitiated(eq(10L), any())).thenReturn(sampleResponse());
        when(purchaseOrderManagementService.markPaymentCompleted(eq(10L), any())).thenReturn(sampleResponse());
        when(purchaseOrderManagementService.cancelPurchaseOrder(eq(10L), any(), eq(77L))).thenReturn(sampleResponse());
        when(purchaseOrderManagementService.receivePurchaseOrder(eq(10L), any(), eq(77L))).thenReturn(sampleResponse());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/purchase-orders/10")
                        .with(authentication(authenticatedUser(77L, "OFFICER")))
                        .contentType(APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/purchase-orders/10/submit")
                        .with(authentication(authenticatedUser(77L, "OFFICER")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"remarks\":\"Ready\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/purchase-orders/10/submit-for-payment")
                        .with(authentication(authenticatedUser(77L, "OFFICER"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/purchase-orders/10/approve")
                        .with(authentication(authenticatedUser(77L, "MANAGER")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"approvalRemarks\":\"Approved\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/purchase-orders/10/payment-initiated")
                        .with(authentication(authenticatedUser(77L, "MANAGER")))
                        .contentType(APPLICATION_JSON)
                        .content(validPaymentTransitionRequest()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/purchase-orders/10/payment-completed")
                        .with(authentication(authenticatedUser(77L, "MANAGER")))
                        .contentType(APPLICATION_JSON)
                        .content(validPaymentTransitionRequest()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/purchase-orders/10/cancel")
                        .with(authentication(authenticatedUser(77L, "ADMIN")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"cancellationReason\":\"Supplier delay\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/purchase-orders/10/receive")
                        .with(authentication(authenticatedUser(77L, "STAFF")))
                        .contentType(APPLICATION_JSON)
                        .content(validReceiveRequest()))
                .andExpect(status().isOk());
    }

    @Test
    void paymentCompletedReturnsBadRequestForValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders/10/payment-completed")
                        .with(authentication(authenticatedUser(77L, "MANAGER")))
                        .contentType(APPLICATION_JSON)
                        .content("{\"paymentStatus\":\"\",\"actorId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.paymentStatus").exists())
                .andExpect(jsonPath("$.actorId").exists());
    }

    private UsernamePasswordAuthenticationToken authenticatedUser(Long userId, String role) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "user@example.com", role, "token");
        return new UsernamePasswordAuthenticationToken(
                principal,
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private String validCreateRequest() {
        return """
                {
                  "supplierId": 1,
                  "warehouseId": 2,
                  "expectedDeliveryDate": "2026-05-20",
                  "paymentTerms": "NET30",
                  "notes": "Urgent",
                  "lineItems": [
                    {
                      "productId": 100,
                      "orderedQuantity": 3,
                      "unitCost": 45.50,
                      "notes": "Primary item"
                    }
                  ]
                }
                """;
    }

    private String validUpdateRequest() {
        return """
                {
                  "supplierId": 1,
                  "warehouseId": 2,
                  "expectedDeliveryDate": "2026-05-22",
                  "paymentTerms": "NET15",
                  "notes": "Updated",
                  "taxAmount": 5.00,
                  "discountAmount": 0.00,
                  "shippingAmount": 0.00,
                  "lineItems": [
                    {
                      "productId": 100,
                      "orderedQuantity": 2,
                      "unitCost": 50.00,
                      "notes": "Updated item"
                    }
                  ]
                }
                """;
    }

    private String validPaymentTransitionRequest() {
        return """
                {
                  "paymentStatus": "PAID",
                  "paymentId": 10,
                  "paymentNumber": "PAY-10",
                  "razorpayOrderId": "order-1",
                  "razorpayPaymentId": "payment-1",
                  "paidAt": "2026-05-09T10:15:30",
                  "actorId": 77
                }
                """;
    }

    private String validReceiveRequest() {
        return """
                {
                  "receiptReference": "GRN-10",
                  "receivedDate": "2026-05-09",
                  "notes": "Receipt completed",
                  "lineItems": [
                    {
                      "lineItemId": 1,
                      "productId": 100,
                      "receivedQuantity": 2,
                      "unitCost": 50.00,
                      "notes": "Received"
                    }
                  ]
                }
                """;
    }

    private PurchaseOrderResponse sampleResponse() {
        return new PurchaseOrderResponse(
                10L,
                "PO-20260509-0001",
                1L,
                "Acme Supplies",
                2L,
                "Central Warehouse",
                77L,
                null,
                null,
                null,
                "DRAFT",
                BigDecimal.valueOf(136.50),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(136.50),
                LocalDate.of(2026, 5, 20),
                null,
                "NET30",
                "Urgent",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 5, 9, 12, 0),
                LocalDateTime.of(2026, 5, 9, 12, 0),
                false,
                null,
                false,
                List.of(),
                List.of());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }
}

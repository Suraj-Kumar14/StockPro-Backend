package com.stockpro.paymentservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.paymentservice.dto.request.RazorpayInitiateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayPaymentStatusUpdateRequest;
import com.stockpro.paymentservice.dto.request.RazorpayVerifyRequest;
import com.stockpro.paymentservice.dto.request.SplitPaymentPlanRequest;
import com.stockpro.paymentservice.dto.response.PaymentResponse;
import com.stockpro.paymentservice.dto.response.PaymentSummaryResponse;
import com.stockpro.paymentservice.dto.response.RazorpayOrderResponse;
import com.stockpro.paymentservice.dto.response.RemainingAmountResponse;
import com.stockpro.paymentservice.dto.response.SplitPaymentPlanResponse;
import com.stockpro.paymentservice.enums.PaymentMethod;
import com.stockpro.paymentservice.enums.PaymentStatus;
import com.stockpro.paymentservice.exception.GlobalExceptionHandler;
import com.stockpro.paymentservice.publisher.SystemAlertPublisher;
import com.stockpro.paymentservice.security.AuthenticatedUser;
import com.stockpro.paymentservice.service.PaymentService;
import com.stockpro.paymentservice.service.RazorpayPaymentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

@WebMvcTest(PaymentController.class)
@Import({PaymentControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private PaymentService paymentService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private RazorpayPaymentService razorpayPaymentService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private SystemAlertPublisher systemAlertPublisher;

    @Test
    void getAllPayments_shouldReturnPageForManager() throws Exception {
        when(paymentService.getAllPayments(0, 10, "createdAt", "desc"))
                .thenReturn(new PageImpl<>(List.of(paymentResponse())));

        mockMvc.perform(get("/api/v1/payments").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentNumber").value("PAY-001"));
    }

    @Test
    void getPaymentSummary_shouldRejectStaffRole() throws Exception {
        mockMvc.perform(get("/api/v1/payments/summary").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRemainingAmount_shouldReturnBody() throws Exception {
        when(razorpayPaymentService.getRemainingAmount(101L, "Bearer token")).thenReturn(
                RemainingAmountResponse.builder()
                        .purchaseOrderId(101L)
                        .totalAmount(new BigDecimal("1000.00"))
                        .paidAmount(new BigDecimal("400.00"))
                        .remainingAmount(new BigDecimal("600.00"))
                        .status(PaymentStatus.PARTIALLY_PAID)
                        .maxAllowedAmount(new BigDecimal("500000.00"))
                        .currency("INR")
                        .build());

        mockMvc.perform(get("/api/v1/payments/purchase-order/101/remaining-amount")
                        .header("Authorization", "Bearer token")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingAmount").value(600.00))
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void searchPaymentsAndGetPaymentById_shouldReturnBodies() throws Exception {
        when(paymentService.searchPayments(7L, PaymentStatus.PAID, LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 9), 0, 10, "createdAt", "desc"))
                .thenReturn(new PageImpl<>(List.of(paymentResponse())));
        when(paymentService.getPaymentById(1L)).thenReturn(paymentResponse());

        mockMvc.perform(get("/api/v1/payments/search")
                        .param("supplierId", "7")
                        .param("status", "PAID")
                        .param("fromDate", "2026-05-01")
                        .param("toDate", "2026-05-09")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("INITIATED"));

        mockMvc.perform(get("/api/v1/payments/1").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentNumber").value("PAY-001"));
    }

    @Test
    void purchaseOrderPaymentEndpoints_shouldReturnPaidAndPagedResults() throws Exception {
        when(paymentService.getPaymentsByPurchaseOrder(101L, 0, 10))
                .thenReturn(new PageImpl<>(List.of(paymentResponse())));
        when(paymentService.getPaidAmountForPurchaseOrder(101L)).thenReturn(new BigDecimal("400.00"));

        mockMvc.perform(get("/api/v1/payments/purchase-order/101").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentNumber").value("PAY-001"));

        mockMvc.perform(get("/api/v1/payments/purchase-order/101/paid-amount")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(400.00));
    }

    @Test
    void initiateAndVerifyRazorpayPayment_shouldUseAuthenticatedActorId() throws Exception {
        UsernamePasswordAuthenticationToken auth = authenticationToken(77L, "MANAGER");

        when(razorpayPaymentService.initiatePayment(any(RazorpayInitiateRequest.class), eq(77L), eq("Bearer token"), eq(false)))
                .thenReturn(RazorpayOrderResponse.builder()
                        .razorpayOrderId("order_123")
                        .paymentNumber("PAY-001")
                        .purchaseOrderId(101L)
                        .amount(new BigDecimal("400.00"))
                        .currency("INR")
                        .keyId("rzp_test")
                        .description("PO payment")
                        .build());
        when(razorpayPaymentService.verifyPayment(any(RazorpayVerifyRequest.class), eq(77L), eq("Bearer token")))
                .thenReturn(paymentResponse());

        mockMvc.perform(post("/api/v1/payments/razorpay/initiate")
                        .with(authentication(auth))
                        .header("Authorization", "Bearer token")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RazorpayInitiateRequest(101L, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_123"));

        mockMvc.perform(post("/api/v1/payments/razorpay/verify")
                        .with(authentication(auth))
                        .header("Authorization", "Bearer token")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RazorpayVerifyRequest("order_123", "pay_123", "sig_123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INITIATED"));

        verify(razorpayPaymentService).initiatePayment(any(RazorpayInitiateRequest.class), eq(77L), eq("Bearer token"), eq(false));
        verify(razorpayPaymentService).verifyPayment(any(RazorpayVerifyRequest.class), eq(77L), eq("Bearer token"));
    }

    @Test
    void splitPlan_shouldBeAvailableOnlyToAdmin() throws Exception {
        when(razorpayPaymentService.getSplitPaymentPlan(any(SplitPaymentPlanRequest.class), eq("Bearer token")))
                .thenReturn(SplitPaymentPlanResponse.builder()
                        .purchaseOrderId(101L)
                        .totalAmount(new BigDecimal("600000.00"))
                        .requestedAmount(new BigDecimal("600000.00"))
                        .remainingAmount(new BigDecimal("600000.00"))
                        .maxAllowedAmount(new BigDecimal("500000.00"))
                        .suggestedSplits(List.of(new BigDecimal("500000.00"), new BigDecimal("100000.00")))
                        .build());

        mockMvc.perform(post("/api/v1/payments/split-plan")
                        .with(user("admin").roles("ADMIN"))
                        .header("Authorization", "Bearer token")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SplitPaymentPlanRequest(101L, new BigDecimal("600000.00")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAllowedAmount").value(500000.00))
                .andExpect(jsonPath("$.suggestedSplits[0]").value(500000.00));

        mockMvc.perform(post("/api/v1/payments/split-plan")
                        .with(user("officer").roles("OFFICER"))
                        .header("Authorization", "Bearer token")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SplitPaymentPlanRequest(101L, new BigDecimal("600000.00")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void failureAndCancellationEndpoints_shouldUseAuthenticatedActorId() throws Exception {
        UsernamePasswordAuthenticationToken auth = authenticationToken(77L, "MANAGER");

        when(razorpayPaymentService.recordFailedPayment(any(RazorpayPaymentStatusUpdateRequest.class), eq(77L)))
                .thenReturn(paymentResponse());
        when(razorpayPaymentService.recordCancelledPayment(any(RazorpayPaymentStatusUpdateRequest.class), eq(77L)))
                .thenReturn(paymentResponse());

        mockMvc.perform(post("/api/v1/payments/razorpay/failure")
                        .with(authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RazorpayPaymentStatusUpdateRequest("order_123", "pay_123", "Failed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentNumber").value("PAY-001"));

        mockMvc.perform(post("/api/v1/payments/razorpay/cancel")
                        .with(authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RazorpayPaymentStatusUpdateRequest("order_123", null, "Cancelled"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentNumber").value("PAY-001"));
    }

    @Test
    void initiateRazorpayPayment_shouldReturnBadRequestForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/payments/razorpay/initiate")
                        .with(user("manager").roles("MANAGER"))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid or missing purchaseOrderId"));
    }

    @Test
    void verifyRazorpayPayment_shouldReturnBadRequestForInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/payments/razorpay/verify")
                        .with(user("manager").roles("MANAGER"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"razorpayOrderId\":\"\",\"razorpayPaymentId\":\"\",\"razorpaySignature\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
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

    private UsernamePasswordAuthenticationToken authenticationToken(Long userId, String role) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "user@example.com", role, "token"),
                "token",
                AuthorityUtils.createAuthorityList("ROLE_" + role));
    }

    private PaymentResponse paymentResponse() {
        return PaymentResponse.builder()
                .paymentId(1L)
                .paymentNumber("PAY-001")
                .purchaseOrderId(101L)
                .poNumber("PO-101")
                .supplierId(7L)
                .supplierName("Acme")
                .status(PaymentStatus.INITIATED)
                .paymentMethod(PaymentMethod.RAZORPAY)
                .paymentAmount(new BigDecimal("400.00"))
                .poTotalAmount(new BigDecimal("1000.00"))
                .previouslyPaidAmount(new BigDecimal("0.00"))
                .remainingAmount(new BigDecimal("600.00"))
                .currency("INR")
                .paymentDate(LocalDate.of(2026, 5, 9))
                .transactionReference("TXN-001")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

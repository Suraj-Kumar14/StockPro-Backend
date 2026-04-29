package com.stockpro.paymentservice;

import com.stockpro.paymentservice.dto.*;
import com.stockpro.paymentservice.entity.*;
import com.stockpro.paymentservice.exception.PaymentException;
import com.stockpro.paymentservice.repository.PaymentRepository;
import com.stockpro.paymentservice.service.PaymentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    // Mock RazorpayClient to avoid real API calls in tests
    @Mock
    private com.razorpay.RazorpayClient razorpayClient;

    @InjectMocks
    private PaymentService paymentService;

    private Payment mockPayment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService,
                "razorpayKeyId", "test_key_id");
        ReflectionTestUtils.setField(paymentService,
                "razorpayKeySecret", "test_key_secret");
        ReflectionTestUtils.setField(paymentService,
                "defaultCurrency", "INR");

        mockPayment = Payment.builder()
                .paymentId(1L)
                .razorpayOrderId("order_test123")
                .purchaseOrderId(1L)
                .userId(1L)
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .description("Payment for PO-1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getPaymentById_Success() {
        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(mockPayment));

        PaymentResponseDTO result = paymentService.getPaymentById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPaymentId());
        assertEquals("order_test123", result.getRazorpayOrderId());
        assertEquals(PaymentStatus.CREATED, result.getStatus());
    }

    @Test
    void getPaymentById_NotFound_ThrowsException() {
        when(paymentRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(PaymentException.class,
                () -> paymentService.getPaymentById(99L));
    }

    @Test
    void getPaymentByOrderId_Success() {
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(mockPayment));

        PaymentResponseDTO result =
                paymentService.getPaymentByOrderId("order_test123");

        assertNotNull(result);
        assertEquals("order_test123", result.getRazorpayOrderId());
    }

    @Test
    void getPaymentByOrderId_NotFound_ThrowsException() {
        when(paymentRepository.findByRazorpayOrderId("invalid_order"))
                .thenReturn(Optional.empty());

        assertThrows(PaymentException.class,
                () -> paymentService.getPaymentByOrderId("invalid_order"));
    }

    @Test
    void getPaymentsByPO_ReturnsList() {
        when(paymentRepository.findByPurchaseOrderId(1L))
                .thenReturn(List.of(mockPayment));

        List<PaymentResponseDTO> result =
                paymentService.getPaymentsByPO(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getPurchaseOrderId());
    }

    @Test
    void getPaymentsByUser_ReturnsList() {
        when(paymentRepository.findByUserId(1L))
                .thenReturn(List.of(mockPayment));

        List<PaymentResponseDTO> result =
                paymentService.getPaymentsByUser(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getAllPayments_ReturnsList() {
        when(paymentRepository.findAll())
                .thenReturn(List.of(mockPayment));

        List<PaymentResponseDTO> result =
                paymentService.getAllPayments();

        assertEquals(1, result.size());
    }

    @Test
    void getAllPayments_EmptyList() {
        when(paymentRepository.findAll()).thenReturn(List.of());

        List<PaymentResponseDTO> result = paymentService.getAllPayments();

        assertTrue(result.isEmpty());
    }

    @Test
    void getPaymentsByStatus_ValidStatus_ReturnsList() {
        when(paymentRepository.findByStatus(PaymentStatus.CREATED))
                .thenReturn(List.of(mockPayment));

        List<PaymentResponseDTO> result =
                paymentService.getPaymentsByStatus("CREATED");

        assertEquals(1, result.size());
        assertEquals(PaymentStatus.CREATED, result.get(0).getStatus());
    }

    @Test
    void getPaymentsByStatus_InvalidStatus_ThrowsException() {
        assertThrows(PaymentException.class,
                () -> paymentService.getPaymentsByStatus("INVALID"));
    }

    @Test
    void verifyPayment_OrderNotFound_ThrowsException() {
        when(paymentRepository.findByRazorpayOrderId("bad_order"))
                .thenReturn(Optional.empty());

        PaymentVerificationDTO dto = new PaymentVerificationDTO();
        dto.setRazorpayOrderId("bad_order");
        dto.setRazorpayPaymentId("pay_123");
        dto.setRazorpaySignature("sig_123");

        assertThrows(PaymentException.class,
                () -> paymentService.verifyPayment(dto));
    }

    @Test
    void verifyPayment_InvalidSignature_ThrowsException() {
        when(paymentRepository.findByRazorpayOrderId("order_test123"))
                .thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(mockPayment);

        PaymentVerificationDTO dto = new PaymentVerificationDTO();
        dto.setRazorpayOrderId("order_test123");
        dto.setRazorpayPaymentId("pay_123");
        dto.setRazorpaySignature("invalid_signature");

        assertThrows(PaymentException.class,
                () -> paymentService.verifyPayment(dto));

        // Should mark payment as FAILED
        verify(paymentRepository).save(
                argThat(p -> p.getStatus() == PaymentStatus.FAILED));
    }

    @Test
    void getPaymentsByStatus_Success_ReturnsList() {
        when(paymentRepository.findByStatus(PaymentStatus.SUCCESS))
                .thenReturn(List.of());

        List<PaymentResponseDTO> result =
                paymentService.getPaymentsByStatus("SUCCESS");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
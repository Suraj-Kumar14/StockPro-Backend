package com.stockpro.purchaseservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.purchaseservice.dto.PaymentStatusSnapshotDTO;
import com.stockpro.purchaseservice.dto.StockProductThresholdDTO;
import com.stockpro.purchaseservice.dto.SupplierLookupResponseDTO;
import com.stockpro.purchaseservice.dto.WarehouseLookupResponseDTO;
import com.stockpro.purchaseservice.dto.request.ApprovePurchaseOrderRequest;
import com.stockpro.purchaseservice.dto.request.CancelPurchaseOrderRequest;
import com.stockpro.purchaseservice.dto.request.CreatePurchaseOrderLineItemRequest;
import com.stockpro.purchaseservice.dto.request.CreatePurchaseOrderRequest;
import com.stockpro.purchaseservice.dto.request.PaymentTransitionRequest;
import com.stockpro.purchaseservice.dto.request.ReceivePurchaseOrderLineItemRequest;
import com.stockpro.purchaseservice.dto.request.ReceivePurchaseOrderRequest;
import com.stockpro.purchaseservice.dto.request.SubmitPurchaseOrderRequest;
import com.stockpro.purchaseservice.dto.request.UpdatePurchaseOrderRequest;
import com.stockpro.purchaseservice.dto.response.PurchaseAnalyticsResponse;
import com.stockpro.purchaseservice.dto.response.PurchaseOrderReportRowResponse;
import com.stockpro.purchaseservice.dto.response.PurchaseOrderResponse;
import com.stockpro.purchaseservice.dto.response.PurchaseOrderSummaryResponse;
import com.stockpro.purchaseservice.entity.POLineItem;
import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.entity.PurchaseOrder;
import com.stockpro.purchaseservice.entity.PurchaseOrderHistory;
import com.stockpro.purchaseservice.enums.PurchaseOrderAction;
import com.stockpro.purchaseservice.events.PurchaseEvent;
import com.stockpro.purchaseservice.exception.InvalidPOStateException;
import com.stockpro.purchaseservice.exception.InvalidReceiveQuantityException;
import com.stockpro.purchaseservice.repository.POLineItemRepository;
import com.stockpro.purchaseservice.repository.PurchaseOrderHistoryRepository;
import com.stockpro.purchaseservice.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderManagementServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private POLineItemRepository poLineItemRepository;

    @Mock
    private PurchaseOrderHistoryRepository historyRepository;

    @Mock
    private SupplierGateway supplierGateway;

    @Mock
    private WarehouseGateway warehouseGateway;

    @Mock
    private ProductCatalogGateway productGateway;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PurchaseEventPublisher purchaseEventPublisher;

    @Captor
    private ArgumentCaptor<PurchaseOrderHistory> historyCaptor;

    @Captor
    private ArgumentCaptor<PurchaseEvent> eventCaptor;

    private PurchaseOrderManagementService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderManagementService(
                purchaseOrderRepository,
                poLineItemRepository,
                historyRepository,
                supplierGateway,
                warehouseGateway,
                productGateway,
                paymentGateway,
                purchaseEventPublisher);
        setRouting("createdRouting", "purchase.created");
        setRouting("updatedRouting", "purchase.updated");
        setRouting("submittedRouting", "purchase.submitted");
        setRouting("approvedRouting", "purchase.approved");
        setRouting("rejectedRouting", "purchase.rejected");
        setRouting("cancelledRouting", "purchase.cancelled");
        setRouting("partiallyReceivedRouting", "purchase.partially-received");
        setRouting("fullyReceivedRouting", "purchase.fully-received");
        setRouting("overdueRouting", "purchase.overdue");
        setRouting("pendingApprovalRouting", "purchase.pending-approval");

        lenient().when(historyRepository.findByPurchaseOrderIdOrderByActionAtAsc(any()))
                .thenReturn(List.of());
        lenient().when(supplierGateway.getSupplier(any())).thenAnswer(invocation -> supplier(invocation.getArgument(0)));
        lenient().when(warehouseGateway.getWarehouse(any())).thenAnswer(invocation -> warehouse(invocation.getArgument(0)));
    }

    @Test
    void createPurchaseOrderCreatesDraftWithCalculatedTotalsAndPublishesEvent() {
        CreatePurchaseOrderRequest request = createRequest();
        when(purchaseOrderRepository.existsByPoNumber(anyString())).thenReturn(false);
        when(productGateway.getProductDetails(100L)).thenReturn(product(100L, "SKU-100", "Widget", true));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder order = invocation.getArgument(0);
            order.setPoId(10L);
            return order;
        });

        PurchaseOrderResponse response = service.createPurchaseOrder(request, 55L);

        assertEquals(10L, response.purchaseOrderId());
        assertEquals("DRAFT", response.status());
        assertTrue(BigDecimal.valueOf(100).compareTo(response.totalAmount()) == 0);
        assertEquals("Acme-1", response.supplierName());
        assertNotNull(response.poNumber());
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(PurchaseOrderAction.CREATED.name(), historyCaptor.getValue().getAction());
        verify(purchaseEventPublisher).publish(eq("purchase.created"), eventCaptor.capture());
        assertEquals("DRAFT", eventCaptor.getValue().newStatus());
    }

    @Test
    void updatePurchaseOrderRejectsNonEditableStatuses() {
        PurchaseOrder order = baseOrder(POStatus.APPROVED);
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(InvalidPOStateException.class, () -> service.updatePurchaseOrder(10L, updateRequest(), 11L));

        verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void updatePurchaseOrderRecalculatesAmountsAndPublishesUpdateEvent() {
        PurchaseOrder order = baseOrder(POStatus.DRAFT);
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(productGateway.getProductDetails(100L)).thenReturn(product(100L, "SKU-100", "Widget", true));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponse response = service.updatePurchaseOrder(10L, updateRequest(), 22L);

        assertEquals("DRAFT", response.status());
        assertTrue(BigDecimal.valueOf(51).compareTo(response.totalAmount()) == 0);
        verify(purchaseEventPublisher).publish(eq("purchase.updated"), any(PurchaseEvent.class));
    }

    @Test
    void getPurchaseOrdersByStatusNormalizesReceivedAndFullyReceived() {
        PurchaseOrder received = baseOrder(POStatus.RECEIVED);
        received.setPoId(1L);
        PurchaseOrder fullyReceived = baseOrder(POStatus.FULLY_RECEIVED);
        fullyReceived.setPoId(2L);
        when(purchaseOrderRepository.findAllByStatusIn(anyCollection())).thenReturn(List.of(received, fullyReceived));

        List<PurchaseOrderResponse> responses = service.getPurchaseOrdersByStatus(POStatus.RECEIVED);

        assertEquals(2, responses.size());
        assertEquals(List.of("RECEIVED", "RECEIVED"), responses.stream().map(PurchaseOrderResponse::status).toList());
    }

    @Test
    void submitPurchaseOrderMovesDraftToPendingApproval() {
        PurchaseOrder order = baseOrder(POStatus.DRAFT);
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.saveAndFlush(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponse response = service.submitPurchaseOrder(10L, new SubmitPurchaseOrderRequest("Ready"), 55L);

        assertEquals("PENDING_APPROVAL", response.status());
        assertNotNull(response.submittedAt());
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(PurchaseOrderAction.SUBMITTED.name(), historyCaptor.getValue().getAction());
        assertEquals("Ready", historyCaptor.getValue().getRemarks());
    }

    @Test
    void submitForPaymentAndPaymentTransitionsUpdateStatuses() {
        PurchaseOrder approved = baseOrder(POStatus.APPROVED);
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(approved));
        when(purchaseOrderRepository.saveAndFlush(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponse pendingPayment = service.submitForPayment(10L, 55L);
        assertEquals("PENDING_PAYMENT", pendingPayment.status());

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(approved));
        approved.setStatus(POStatus.PENDING_PAYMENT);
        PurchaseOrderResponse initiated = service.markPaymentInitiated(10L, new PaymentTransitionRequest(
                "INITIATED", 1L, "PAY-1", "order-1", null, null, 55L));
        assertEquals("PAYMENT_INITIATED", initiated.status());

        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(approved));
        approved.setStatus(POStatus.PAYMENT_INITIATED);
        PurchaseOrderResponse paid = service.markPaymentCompleted(10L, new PaymentTransitionRequest(
                "PAID", 1L, "PAY-1", "order-1", "payment-1", LocalDateTime.now(), 55L));
        assertEquals("PAID", paid.status());
    }

    @Test
    void approvePurchaseOrderMovesPendingApprovalToApproved() {
        PurchaseOrder order = baseOrder(POStatus.PENDING_APPROVAL);
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponse response = service.approvePurchaseOrder(10L, new ApprovePurchaseOrderRequest("Looks good"), 90L);

        assertEquals("APPROVED", response.status());
        assertEquals(90L, response.approvedBy());
        assertEquals("Looks good", response.approvalRemarks());
    }

    @Test
    void cancelPurchaseOrderRejectsApprovedOrdersAfterReceiptStarts() {
        PurchaseOrder order = baseOrder(POStatus.APPROVED);
        order.setLineItems(List.of(lineItem(1L, 100L, 10, 1, "25.00")));
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(InvalidPOStateException.class,
                () -> service.cancelPurchaseOrder(10L, new CancelPurchaseOrderRequest("Stop"), 70L));
    }

    @Test
    void rejectAndCancelPurchaseOrderPersistAuditFields() {
        PurchaseOrder pending = baseOrder(POStatus.PENDING_APPROVAL);
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(pending));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponse rejected = service.rejectPurchaseOrder(
                10L, new com.stockpro.purchaseservice.dto.request.RejectPurchaseOrderRequest("Insufficient budget"), 80L);
        assertEquals("REJECTED", rejected.status());
        assertEquals("Insufficient budget", rejected.rejectionReason());

        PurchaseOrder draft = baseOrder(POStatus.DRAFT);
        when(purchaseOrderRepository.findById(11L)).thenReturn(Optional.of(draft));
        PurchaseOrderResponse cancelled = service.cancelPurchaseOrder(
                11L, new CancelPurchaseOrderRequest("Supplier changed"), 81L);
        assertEquals("CANCELLED", cancelled.status());
        assertEquals("Supplier changed", cancelled.cancellationReason());
    }

    @Test
    void receivePurchaseOrderMarksPartiallyReceivedAndUpdatesWarehouse() {
        PurchaseOrder order = baseOrder(POStatus.PAID);
        order.setPoId(10L);
        order.setWarehouseId(2L);
        POLineItem lineItem = lineItem(1L, 100L, 10, 0, "25.00");
        lineItem.setPurchaseOrder(order);
        order.setLineItems(List.of(lineItem));
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(poLineItemRepository.findByPurchaseOrderPoId(10L)).thenReturn(List.of(lineItem));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReceivePurchaseOrderRequest request = new ReceivePurchaseOrderRequest(
                "GRN-100",
                LocalDate.of(2026, 5, 9),
                "Partial receipt",
                List.of(new ReceivePurchaseOrderLineItemRequest(1L, 100L, 4, BigDecimal.valueOf(25), "Received four units")));

        PurchaseOrderResponse response = service.receivePurchaseOrder(10L, request, 200L);

        assertEquals("PARTIALLY_RECEIVED", response.status());
        assertEquals(LocalDate.of(2026, 5, 9), response.actualDeliveryDate());
        assertEquals(4, response.lineItems().get(0).receivedQuantity());
        verify(warehouseGateway).increaseStock(eq(2L), eq(100L), eq(4), eq(10L), eq(order.getPoNumber()),
                eq(BigDecimal.valueOf(25)), eq("Received four units"), eq(null));
        verify(historyRepository).save(historyCaptor.capture());
        assertEquals(PurchaseOrderAction.PARTIALLY_RECEIVED.name(), historyCaptor.getValue().getAction());
    }

    @Test
    void receivePurchaseOrderRejectsMismatchedProductIds() {
        PurchaseOrder order = baseOrder(POStatus.PAID);
        order.setPoId(10L);
        POLineItem lineItem = lineItem(1L, 100L, 10, 0, "25.00");
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(poLineItemRepository.findByPurchaseOrderPoId(10L)).thenReturn(List.of(lineItem));

        ReceivePurchaseOrderRequest request = new ReceivePurchaseOrderRequest(
                "GRN-100",
                LocalDate.of(2026, 5, 9),
                "Mismatch",
                List.of(new ReceivePurchaseOrderLineItemRequest(1L, 999L, 2, BigDecimal.valueOf(25), "Wrong product")));

        assertThrows(InvalidReceiveQuantityException.class, () -> service.receivePurchaseOrder(10L, request, 200L));
    }

    @Test
    void getPurchaseOrderReportsFiltersByRequestedPaymentStatus() {
        PurchaseOrder order = baseOrder(POStatus.APPROVED);
        order.setPoId(10L);
        order.setLineItems(List.of(lineItem(1L, 100L, 5, 2, "25.00")));
        when(purchaseOrderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(paymentGateway.getPaymentStatusSnapshot(10L)).thenReturn(PaymentStatusSnapshotDTO.builder()
                .paymentStatus("PAID")
                .paymentNumber("PAY-10")
                .paymentCompleted(true)
                .paymentAmount(BigDecimal.valueOf(125))
                .build());

        List<PurchaseOrderReportRowResponse> rows = service
                .getPurchaseOrderReports("PO", POStatus.APPROVED, "PAID", 1L, null, null, 0, 10)
                .getContent();

        assertEquals(1, rows.size());
        assertEquals("PAID", rows.get(0).paymentStatus());
        assertEquals(3, rows.get(0).remainingQuantity());
    }

    @Test
    void basicReadAndSearchOperationsMapRepositoryResults() {
        PurchaseOrder order = baseOrder(POStatus.DRAFT);
        when(purchaseOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findByPoNumber("PO-20260509-0001")).thenReturn(Optional.of(order));
        when(purchaseOrderRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        when(purchaseOrderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1));
        PurchaseOrderHistory history = PurchaseOrderHistory.builder()
                .historyId(1L)
                .purchaseOrderId(10L)
                .action("CREATED")
                .newStatus("DRAFT")
                .actorId(55L)
                .remarks("created")
                .build();
        when(historyRepository.findByPurchaseOrderIdOrderByActionAtAsc(10L)).thenReturn(List.of(history));

        assertEquals("PO-20260509-0001", service.getPurchaseOrderById(10L).poNumber());
        assertEquals("PO-20260509-0001", service.getPurchaseOrderByNumber("PO-20260509-0001").poNumber());
        assertEquals(1, service.getAllPurchaseOrders(0, 10, "createdAt", "desc").getTotalElements());
        assertEquals(1, service.searchPurchaseOrders("PO", 1L, 2L, POStatus.DRAFT, 55L,
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(1), false, 0, 10, "createdAt", "desc").getTotalElements());
        assertEquals(1, service.getPurchaseOrderHistory(10L).size());
    }

    @Test
    void summaryAndAlertsQueriesAggregateRepositoryData() {
        PurchaseOrder draft = baseOrder(POStatus.DRAFT);
        PurchaseOrder pending = baseOrder(POStatus.PENDING_APPROVAL);
        pending.setTotalAmount(BigDecimal.valueOf(80));
        PurchaseOrder received = baseOrder(POStatus.RECEIVED);
        received.setTotalAmount(BigDecimal.valueOf(120));
        received.setExpectedDate(LocalDate.now().minusDays(1));

        when(purchaseOrderRepository.findAll()).thenReturn(List.of(draft, pending, received));
        when(purchaseOrderRepository.findByCreatedById(55L)).thenReturn(List.of(pending));
        when(purchaseOrderRepository.findOverduePurchaseOrders(anyCollection(), any())).thenReturn(List.of(received));
        when(purchaseOrderRepository.findByStatus(POStatus.PENDING_APPROVAL)).thenReturn(List.of(pending));

        PurchaseOrderSummaryResponse summary = service.getPurchaseOrderSummary();
        PurchaseOrderSummaryResponse officerSummary = service.getPurchaseOfficerSummary(55L);

        assertEquals(3, summary.totalPurchaseOrders());
        assertEquals(1, summary.pendingApprovalCount());
        assertEquals(1, officerSummary.totalPurchaseOrders());
        assertEquals(1, service.getOverduePurchaseOrders().size());
        assertEquals(1, service.getPendingApprovalPurchaseOrders().size());

        service.publishOverduePurchaseOrders();
        verify(purchaseEventPublisher).publish(eq("purchase.overdue"), any(PurchaseEvent.class));
    }

    @Test
    void getPurchaseAnalyticsAggregatesReceivedOrdersAndLeadTimes() {
        PurchaseOrder received = baseOrder(POStatus.RECEIVED);
        received.setSupplierId(1L);
        received.setTotalAmount(BigDecimal.valueOf(100));
        received.setCreatedAt(LocalDateTime.now().minusDays(2));
        received.setSubmittedAt(LocalDateTime.now().minusDays(4));
        received.setApprovedAt(LocalDateTime.now().minusDays(3));
        received.setExpectedDate(LocalDate.now().minusDays(1));
        received.setActualDeliveryDate(LocalDate.now().plusDays(1));
        received.setLineItems(List.of(lineItem(1L, 100L, 4, 4, "25.00")));

        PurchaseOrder fullyReceived = baseOrder(POStatus.FULLY_RECEIVED);
        fullyReceived.setSupplierId(2L);
        fullyReceived.setTotalAmount(BigDecimal.valueOf(200));
        fullyReceived.setCreatedAt(LocalDateTime.now());
        fullyReceived.setExpectedDate(LocalDate.now().minusDays(2));
        fullyReceived.setActualDeliveryDate(LocalDate.now());
        fullyReceived.setLineItems(List.of(lineItem(2L, 101L, 8, 8, "25.00")));

        PurchaseOrder pending = baseOrder(POStatus.PENDING_APPROVAL);
        pending.setSupplierId(1L);
        pending.setTotalAmount(BigDecimal.valueOf(50));
        pending.setCreatedAt(LocalDateTime.now());
        pending.setExpectedDate(LocalDate.now().minusDays(5));
        pending.setLineItems(List.of(lineItem(3L, 100L, 1, 0, "50.00")));

        when(purchaseOrderRepository.findAll()).thenReturn(List.of(received, fullyReceived, pending));

        PurchaseAnalyticsResponse response = service.getPurchaseAnalytics(
                YearMonth.now().atDay(1),
                YearMonth.now().atEndOfMonth());

        assertTrue(BigDecimal.valueOf(300).compareTo(response.totalSpend()) == 0);
        assertEquals(1, response.pendingApprovals());
        assertFalse(response.topSuppliers().isEmpty());
        assertEquals(24, response.averageApprovalTime());
        assertEquals(2, response.averageDeliveryDelay());
    }

    @Test
    void getAllPurchaseOrdersRejectsUnsupportedSortField() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAllPurchaseOrders(0, 10, "unsupportedField", "asc"));
    }

    private void setRouting(String fieldName, String value) {
        ReflectionTestUtils.setField(service, fieldName, value);
    }

    private CreatePurchaseOrderRequest createRequest() {
        return new CreatePurchaseOrderRequest(
                1L,
                2L,
                LocalDate.of(2026, 5, 20),
                "NET30",
                "Urgent order",
                List.of(new CreatePurchaseOrderLineItemRequest(100L, 2, BigDecimal.valueOf(50), "Widget")));
    }

    private UpdatePurchaseOrderRequest updateRequest() {
        return new UpdatePurchaseOrderRequest(
                1L,
                2L,
                LocalDate.of(2026, 5, 22),
                "NET15",
                "Updated order",
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(new CreatePurchaseOrderLineItemRequest(100L, 1, BigDecimal.valueOf(50), "Widget")));
    }

    private PurchaseOrder baseOrder(POStatus status) {
        PurchaseOrder order = new PurchaseOrder();
        order.setPoId(10L);
        order.setPoNumber("PO-20260509-0001");
        order.setSupplierId(1L);
        order.setWarehouseId(2L);
        order.setCreatedById(55L);
        order.setStatus(status);
        order.setExpectedDate(LocalDate.now().plusDays(3));
        order.setOrderDate(LocalDate.now().minusDays(1));
        order.setCreatedAt(LocalDateTime.now().minusDays(1));
        order.setUpdatedAt(LocalDateTime.now().minusHours(1));
        order.setSubtotalAmount(BigDecimal.valueOf(100));
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.valueOf(100));
        order.setLineItems(new java.util.ArrayList<>());
        return order;
    }

    private POLineItem lineItem(Long lineItemId, Long productId, Integer quantity, Integer receivedQty, String unitCost) {
        POLineItem item = POLineItem.builder()
                .lineItemId(lineItemId)
                .productId(productId)
                .productSku("SKU-" + productId)
                .productName("Product-" + productId)
                .quantity(quantity)
                .receivedQty(receivedQty)
                .unitCost(new BigDecimal(unitCost))
                .totalCost(new BigDecimal(unitCost).multiply(BigDecimal.valueOf(quantity)))
                .notes("Line note")
                .build();
        item.setPurchaseOrder(null);
        return item;
    }

    private StockProductThresholdDTO product(Long productId, String sku, String name, boolean active) {
        StockProductThresholdDTO dto = new StockProductThresholdDTO();
        dto.setProductId(productId);
        dto.setSku(sku);
        dto.setName(name);
        dto.setIsActive(active);
        dto.setReorderLevel(3);
        dto.setMaxStockLevel(20);
        return dto;
    }

    private SupplierLookupResponseDTO supplier(Long supplierId) {
        SupplierLookupResponseDTO dto = new SupplierLookupResponseDTO();
        dto.setSupplierId(supplierId);
        dto.setName("Acme-" + supplierId);
        dto.setIsActive(true);
        return dto;
    }

    private WarehouseLookupResponseDTO warehouse(Long warehouseId) {
        WarehouseLookupResponseDTO dto = new WarehouseLookupResponseDTO();
        dto.setWarehouseId(warehouseId);
        dto.setName("Warehouse-" + warehouseId);
        dto.setIsActive(true);
        return dto;
    }
}

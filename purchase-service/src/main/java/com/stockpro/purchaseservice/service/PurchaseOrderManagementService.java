package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.StockProductThresholdDTO;
import com.stockpro.purchaseservice.dto.SupplierLookupResponseDTO;
import com.stockpro.purchaseservice.dto.WarehouseLookupResponseDTO;
import com.stockpro.purchaseservice.dto.PaymentStatusSnapshotDTO;
import com.stockpro.purchaseservice.dto.request.*;
import com.stockpro.purchaseservice.dto.response.*;
import com.stockpro.purchaseservice.entity.POLineItem;
import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.entity.PurchaseOrder;
import com.stockpro.purchaseservice.entity.PurchaseOrderHistory;
import com.stockpro.purchaseservice.enums.PurchaseOrderAction;
import com.stockpro.purchaseservice.events.PurchaseEvent;
import com.stockpro.purchaseservice.events.PurchaseEventLineItem;
import com.stockpro.purchaseservice.exception.InvalidPurchaseOrderStatusException;
import com.stockpro.purchaseservice.exception.InvalidPOStateException;
import com.stockpro.purchaseservice.exception.InvalidReceiveQuantityException;
import com.stockpro.purchaseservice.exception.PurchaseOrderNotFoundException;
import com.stockpro.purchaseservice.repository.POLineItemRepository;
import com.stockpro.purchaseservice.repository.PurchaseOrderHistoryRepository;
import com.stockpro.purchaseservice.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderManagementService {

    private static final String PARTIALLY_PAID = "PARTIALLY_PAID";
    private static final EnumSet<POStatus> OVERDUE_STATUSES = EnumSet.of(POStatus.APPROVED, POStatus.PAID, POStatus.PARTIALLY_RECEIVED);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "poId",
            "poNumber",
            "supplierId",
            "warehouseId",
            "createdById",
            "status",
            "totalAmount",
            "orderDate",
            "expectedDate",
            "submittedAt",
            "approvedAt",
            "receivedAt",
            "createdAt",
            "updatedAt"
    );

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final POLineItemRepository poLineItemRepository;
    private final PurchaseOrderHistoryRepository historyRepository;
    private final SupplierGateway supplierGateway;
    private final WarehouseGateway warehouseGateway;
    private final ProductCatalogGateway productGateway;
    private final PaymentGateway paymentGateway;
    private final PurchaseEventPublisher purchaseEventPublisher;

    @Value("${stockpro.rabbitmq.purchase.routing.created}") private String createdRouting;
    @Value("${stockpro.rabbitmq.purchase.routing.updated}") private String updatedRouting;
    @Value("${stockpro.rabbitmq.purchase.routing.submitted}") private String submittedRouting;
    @Value("${stockpro.rabbitmq.purchase.routing.approved}") private String approvedRouting;
    @Value("${stockpro.rabbitmq.purchase.routing.rejected}") private String rejectedRouting;
    @Value("${stockpro.rabbitmq.purchase.routing.cancelled}") private String cancelledRouting;
    @Value("${stockpro.rabbitmq.purchase.routing.partiallyReceived}") private String partiallyReceivedRouting;
    @Value("${stockpro.rabbitmq.purchase.routing.fullyReceived}") private String fullyReceivedRouting;
    @Value("${stockpro.rabbitmq.purchase.routing.overdue}") private String overdueRouting;
    @Value("${stockpro.rabbitmq.purchase.routing.pendingApproval}") private String pendingApprovalRouting;

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request, Long actorId) {
        validateCreateOrUpdate(request.supplierId(), request.warehouseId(), request.lineItems());
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setPoNumber(generatePoNumber());
        purchaseOrder.setCreatedById(actorId);
        purchaseOrder.setStatus(POStatus.DRAFT);
        applyEditableFields(purchaseOrder, request.supplierId(), request.warehouseId(), request.expectedDeliveryDate(),
                request.paymentTerms(), request.notes(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, request.lineItems());
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        saveHistory(saved.getPoId(), PurchaseOrderAction.CREATED, null, saved.getStatus(), actorId, "Purchase order created");
        publish(saved, null, saved.getStatus(), actorId, createdRouting, null);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse updatePurchaseOrder(Long poId, UpdatePurchaseOrderRequest request, Long actorId) {
        PurchaseOrder purchaseOrder = getEntity(poId);
        ensureEditable(purchaseOrder);
        validateCreateOrUpdate(request.supplierId(), request.warehouseId(), request.lineItems());
        POStatus oldStatus = purchaseOrder.getStatus();
        applyEditableFields(purchaseOrder, request.supplierId(), request.warehouseId(), request.expectedDeliveryDate(),
                request.paymentTerms(), request.notes(), defaultMoney(request.taxAmount()), defaultMoney(request.discountAmount()),
                defaultMoney(request.shippingAmount()), request.lineItems());
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        saveHistory(saved.getPoId(), PurchaseOrderAction.UPDATED, oldStatus, saved.getStatus(), actorId, "Purchase order updated");
        publish(saved, oldStatus, saved.getStatus(), actorId, updatedRouting, null);
        return toResponse(saved);
    }

    public PurchaseOrderResponse getPurchaseOrderById(Long poId) {
        return toResponse(getEntity(poId));
    }

    public PurchaseOrderResponse getPurchaseOrderByNumber(String poNumber) {
        return toResponse(purchaseOrderRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found with number: " + poNumber)));
    }

    public Page<PurchaseOrderResponse> getAllPurchaseOrders(int page, int size, String sortBy, String sortDir) {
        return purchaseOrderRepository.findAll(pageable(page, size, sortBy, sortDir)).map(this::toResponse);
    }

    public List<PurchaseOrderResponse> getPurchaseOrdersByStatus(POStatus status) {
        List<POStatus> statuses = normalizeStatus(status) == POStatus.RECEIVED
                ? List.of(POStatus.RECEIVED, POStatus.FULLY_RECEIVED)
                : List.of(normalizeStatus(status));
        return purchaseOrderRepository.findAllByStatusIn(statuses).stream().map(this::toResponse).toList();
    }

    public Page<PurchaseOrderResponse> searchPurchaseOrders(String keyword, Long supplierId, Long warehouseId, POStatus status,
            Long createdBy, LocalDate fromDate, LocalDate toDate, Boolean overdueOnly, int page, int size, String sortBy, String sortDir) {
        Specification<PurchaseOrder> spec = Specification.where(null);
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("poNumber")), pattern));
        }
        if (supplierId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("supplierId"), supplierId));
        if (warehouseId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouseId"), warehouseId));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (createdBy != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("createdById"), createdBy));
        if (fromDate != null) spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
        if (toDate != null) spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toDate.plusDays(1).atStartOfDay().minusNanos(1)));
        if (Boolean.TRUE.equals(overdueOnly)) {
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.lessThan(root.get("expectedDate"), LocalDate.now()),
                    root.get("status").in(OVERDUE_STATUSES)));
        }
        return purchaseOrderRepository.findAll(spec, pageable(page, size, sortBy, sortDir)).map(this::toResponse);
    }

    @Transactional
    public PurchaseOrderResponse submitPurchaseOrder(Long poId, SubmitPurchaseOrderRequest request, Long actorId) {
        PurchaseOrder purchaseOrder = getEntity(poId);
        ensureStatus(purchaseOrder, Set.of(POStatus.DRAFT), "submit");
        POStatus oldStatus = purchaseOrder.getStatus();
        purchaseOrder.setStatus(POStatus.PENDING_APPROVAL);
        purchaseOrder.setSubmittedAt(LocalDateTime.now());
        PurchaseOrder saved = purchaseOrderRepository.saveAndFlush(purchaseOrder);
        saveHistory(saved.getPoId(), PurchaseOrderAction.SUBMITTED, oldStatus, saved.getStatus(), actorId, request != null ? request.remarks() : null);
        log.info("Purchase order submitted for approval. poId={}, actorId={}, fromStatus={}, toStatus={}",
                poId, actorId, oldStatus, saved.getStatus());
        publish(saved, oldStatus, saved.getStatus(), actorId, pendingApprovalRouting, "Purchase order submitted for approval");
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse submitForPayment(Long poId, Long actorId) {
        PurchaseOrder purchaseOrder = getEntity(poId);
        ensureStatus(purchaseOrder, Set.of(POStatus.APPROVED), "submit for payment");
        POStatus oldStatus = purchaseOrder.getStatus();
        purchaseOrder.setStatus(POStatus.PENDING_PAYMENT);
        PurchaseOrder saved = purchaseOrderRepository.saveAndFlush(purchaseOrder);
        saveHistory(saved.getPoId(), PurchaseOrderAction.PAYMENT_REQUESTED, oldStatus, saved.getStatus(), actorId,
                "Purchase order submitted for Razorpay payment");
        log.info("Submit-for-payment requested. poId={}, actorId={}, fromStatus={}, toStatus={}",
                poId, actorId, oldStatus, saved.getStatus());
        publish(saved, oldStatus, saved.getStatus(), actorId, updatedRouting, "PAYMENT_REQUESTED");
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse approvePurchaseOrder(Long poId, ApprovePurchaseOrderRequest request, Long actorId) {
        PurchaseOrder purchaseOrder = getEntity(poId);
        ensureStatus(purchaseOrder, Set.of(POStatus.PENDING_APPROVAL), "approve");
        POStatus oldStatus = purchaseOrder.getStatus();
        purchaseOrder.setStatus(POStatus.APPROVED);
        purchaseOrder.setApprovedBy(actorId);
        purchaseOrder.setApprovedAt(LocalDateTime.now());
        purchaseOrder.setApprovalRemarks(request != null ? request.approvalRemarks() : null);
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        saveHistory(saved.getPoId(), PurchaseOrderAction.APPROVED, oldStatus, saved.getStatus(), actorId, saved.getApprovalRemarks());
        publish(saved, oldStatus, saved.getStatus(), actorId, approvedRouting, saved.getApprovalRemarks());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse markPaymentInitiated(Long poId, PaymentTransitionRequest request) {
        PurchaseOrder purchaseOrder = getEntity(poId);
        ensureStatus(purchaseOrder, Set.of(POStatus.PENDING_PAYMENT, POStatus.PAYMENT_INITIATED, POStatus.PAID), "start payment");
        POStatus oldStatus = purchaseOrder.getStatus();
        purchaseOrder.setStatus(POStatus.PAYMENT_INITIATED);
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        saveHistory(saved.getPoId(), PurchaseOrderAction.PAYMENT_INITIATED, oldStatus, saved.getStatus(), request.actorId(),
                "Razorpay payment initiated" + (request.paymentNumber() != null ? " (" + request.paymentNumber() + ")" : ""));
        publish(saved, oldStatus, saved.getStatus(), request.actorId(), updatedRouting, "PAYMENT_INITIATED");
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse markPaymentCompleted(Long poId, PaymentTransitionRequest request) {
        PurchaseOrder purchaseOrder = getEntity(poId);
        ensureStatus(purchaseOrder, Set.of(POStatus.PENDING_PAYMENT, POStatus.PAYMENT_INITIATED), "complete payment");
        POStatus oldStatus = purchaseOrder.getStatus();
        POStatus nextStatus = PARTIALLY_PAID.equalsIgnoreCase(request.paymentStatus())
                ? POStatus.PAYMENT_INITIATED
                : POStatus.PAID;
        purchaseOrder.setStatus(nextStatus);
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        saveHistory(saved.getPoId(), PurchaseOrderAction.PAYMENT_COMPLETED, oldStatus, saved.getStatus(), request.actorId(),
                (PARTIALLY_PAID.equalsIgnoreCase(request.paymentStatus())
                        ? "Partial Razorpay payment completed"
                        : "Razorpay payment completed")
                        + (request.razorpayPaymentId() != null ? " (" + request.razorpayPaymentId() + ")" : ""));
        log.info("Payment completed for purchase order. poId={}, actorId={}, fromStatus={}, toStatus={}, paymentStatus={}, paymentId={}",
                poId, request.actorId(), oldStatus, saved.getStatus(), request.paymentStatus(), request.razorpayPaymentId());
        publish(saved, oldStatus, saved.getStatus(), request.actorId(), updatedRouting,
                PARTIALLY_PAID.equalsIgnoreCase(request.paymentStatus()) ? "PAYMENT_PARTIALLY_COMPLETED" : "PAYMENT_COMPLETED");
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse rejectPurchaseOrder(Long poId, RejectPurchaseOrderRequest request, Long actorId) {
        PurchaseOrder purchaseOrder = getEntity(poId);
        ensureStatus(purchaseOrder, Set.of(POStatus.PENDING_APPROVAL), "reject");
        POStatus oldStatus = purchaseOrder.getStatus();
        purchaseOrder.setStatus(POStatus.REJECTED);
        purchaseOrder.setRejectedBy(actorId);
        purchaseOrder.setRejectedAt(LocalDateTime.now());
        purchaseOrder.setRejectionReason(request.rejectionReason());
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        saveHistory(saved.getPoId(), PurchaseOrderAction.REJECTED, oldStatus, saved.getStatus(), actorId, request.rejectionReason());
        publish(saved, oldStatus, saved.getStatus(), actorId, rejectedRouting, request.rejectionReason());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(Long poId, CancelPurchaseOrderRequest request, Long actorId) {
        PurchaseOrder purchaseOrder = getEntity(poId);
        if (purchaseOrder.getStatus() == POStatus.RECEIVED || purchaseOrder.getStatus() == POStatus.REJECTED || purchaseOrder.getStatus() == POStatus.CANCELLED) {
            throw new InvalidPOStateException("Cannot cancel a received, rejected, or cancelled purchase order");
        }
        if (purchaseOrder.getStatus() == POStatus.PARTIALLY_RECEIVED) {
            throw new InvalidPOStateException("Partially received purchase orders cannot be cancelled");
        }
        if ((purchaseOrder.getStatus() == POStatus.APPROVED) && hasAnyReceivedQuantity(purchaseOrder)) {
            throw new InvalidPOStateException("Cannot cancel an approved purchase order after receipt has started");
        }
        POStatus oldStatus = purchaseOrder.getStatus();
        purchaseOrder.setStatus(POStatus.CANCELLED);
        purchaseOrder.setCancelledBy(actorId);
        purchaseOrder.setCancelledAt(LocalDateTime.now());
        purchaseOrder.setCancellationReason(request.cancellationReason());
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        saveHistory(saved.getPoId(), PurchaseOrderAction.CANCELLED, oldStatus, saved.getStatus(), actorId, request.cancellationReason());
        publish(saved, oldStatus, saved.getStatus(), actorId, cancelledRouting, request.cancellationReason());
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse receivePurchaseOrder(Long poId, ReceivePurchaseOrderRequest request, Long actorId) {
        log.info("Receive purchase order request started. poId={}, actorId={}", poId, actorId);
        PurchaseOrder purchaseOrder = getEntity(poId);
        ensureReceivableStatus(purchaseOrder);
        if (purchaseOrder.getWarehouseId() == null) {
            throw new InvalidReceiveQuantityException("Warehouse ID is required for purchase order receipt");
        }
        List<POLineItem> lineItems = poLineItemRepository.findByPurchaseOrderPoId(poId);
        if (lineItems.isEmpty()) {
            throw new InvalidReceiveQuantityException("No line items found for purchase order " + poId);
        }
        log.info("Purchase order loaded for receipt. poId={}, status={}, warehouseId={}, lineItemsCount={}",
                poId, purchaseOrder.getStatus(), purchaseOrder.getWarehouseId(), lineItems.size());
        Map<Long, POLineItem> lineItemsById = new HashMap<>();
        for (POLineItem item : lineItems) {
            lineItemsById.put(item.getLineItemId(), item);
        }
        for (ReceivePurchaseOrderLineItemRequest lineRequest : request.lineItems()) {
            POLineItem item = lineItemsById.get(lineRequest.lineItemId());
            if (lineRequest.receivedQuantity() == null || lineRequest.receivedQuantity() <= 0) {
                throw new InvalidReceiveQuantityException("Received quantity must be greater than zero");
            }
            if (item == null) {
                throw new InvalidReceiveQuantityException("Line item " + lineRequest.lineItemId() + " does not belong to purchase order " + poId);
            }
            if (lineRequest.productId() == null) {
                throw new InvalidReceiveQuantityException("Product ID is required for every received line item");
            }
            if (!Objects.equals(item.getProductId(), lineRequest.productId())) {
                throw new InvalidReceiveQuantityException("Receipt line item product does not match purchase order line item");
            }
            int pendingQuantity = pendingQuantity(item);
            if (lineRequest.receivedQuantity() > pendingQuantity) {
                throw new InvalidReceiveQuantityException("Received quantity cannot exceed remaining quantity for line item " + lineRequest.lineItemId());
            }
            log.info("Validated receipt line item. poId={}, lineItemId={}, productId={}, receivedQuantity={}, pendingQuantity={}",
                    poId, lineRequest.lineItemId(), lineRequest.productId(), lineRequest.receivedQuantity(), pendingQuantity);
        }
        for (ReceivePurchaseOrderLineItemRequest lineRequest : request.lineItems()) {
            POLineItem item = lineItemsById.get(lineRequest.lineItemId());
            item.setReceivedQty(defaultQty(item.getReceivedQty()) + lineRequest.receivedQuantity());
            if (lineRequest.unitCost() != null) {
                item.setUnitCost(lineRequest.unitCost());
                item.setTotalCost(lineRequest.unitCost().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            item.setNotes(lineRequest.notes());
            warehouseGateway.increaseStock(
                    purchaseOrder.getWarehouseId(),
                    item.getProductId(),
                    lineRequest.receivedQuantity(),
                    purchaseOrder.getPoId(),
                    purchaseOrder.getPoNumber(),
                    item.getUnitCost(),
                    lineRequest.notes() != null && !lineRequest.notes().isBlank() ? lineRequest.notes() : request.notes(),
                    null);
            log.info("Warehouse stock update completed for receipt line. poId={}, warehouseId={}, productId={}, receivedQuantity={}",
                    poId, purchaseOrder.getWarehouseId(), item.getProductId(), lineRequest.receivedQuantity());
        }
        poLineItemRepository.saveAll(lineItems);
        POStatus oldStatus = purchaseOrder.getStatus();
        purchaseOrder.setReceivedBy(actorId);
        purchaseOrder.setReceivedAt(LocalDateTime.now());
        purchaseOrder.setReceivedDate(request.receivedDate() != null ? request.receivedDate() : LocalDate.now());
        purchaseOrder.setActualDeliveryDate(purchaseOrder.getReceivedDate());
        if (lineItems.stream().allMatch(item -> defaultQty(item.getReceivedQty()) >= defaultQty(item.getQuantity()))) {
            purchaseOrder.setStatus(POStatus.RECEIVED);
        } else {
            purchaseOrder.setStatus(POStatus.PARTIALLY_RECEIVED);
        }
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        PurchaseOrderAction action = saved.getStatus() == POStatus.RECEIVED
                ? PurchaseOrderAction.FULLY_RECEIVED
                : PurchaseOrderAction.PARTIALLY_RECEIVED;
        saveHistory(saved.getPoId(), action, oldStatus, saved.getStatus(), actorId, request.notes());
        publish(saved, oldStatus, saved.getStatus(), actorId,
                saved.getStatus() == POStatus.RECEIVED ? fullyReceivedRouting : partiallyReceivedRouting,
                request.notes());
        log.info("Receive purchase order request completed. poId={}, finalStatus={}, warehouseId={}",
                poId, saved.getStatus(), saved.getWarehouseId());
        return toResponse(saved);
    }

    public List<PurchaseOrderHistoryResponse> getPurchaseOrderHistory(Long poId) {
        getEntity(poId);
        return historyRepository.findByPurchaseOrderIdOrderByActionAtAsc(poId).stream().map(this::toHistoryResponse).toList();
    }

    public PurchaseOrderSummaryResponse getPurchaseOrderSummary() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll();
        return buildSummary(orders);
    }

    public PurchaseOrderSummaryResponse getPurchaseOfficerSummary(Long actorId) {
        List<PurchaseOrder> orders = actorId != null
                ? purchaseOrderRepository.findByCreatedById(actorId)
                : List.of();
        return buildSummary(orders);
    }

    public Page<PurchaseOrderReportRowResponse> getPurchaseOrderReports(String keyword, POStatus status, String paymentStatus,
            Long supplierId, LocalDate fromDate, LocalDate toDate, int page, int size) {
        Specification<PurchaseOrder> spec = Specification.where(null);
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("poNumber")), pattern),
                    cb.like(cb.lower(root.get("notes")), pattern)));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (supplierId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("supplierId"), supplierId));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
        }

        Page<PurchaseOrder> orders = purchaseOrderRepository.findAll(spec, pageable(page, size, "createdAt", "desc"));
        List<PurchaseOrderReportRowResponse> rows = orders.getContent().stream()
                .flatMap(order -> {
                    PaymentStatusSnapshotDTO paymentSnapshot = paymentGateway.getPaymentStatusSnapshot(order.getPoId());
                    SupplierLookupResponseDTO supplier = null;
                    WarehouseLookupResponseDTO warehouse = null;
                    try { supplier = supplierGateway.getSupplier(order.getSupplierId()); } catch (Exception ignored) { }
                    try { warehouse = warehouseGateway.getWarehouse(order.getWarehouseId()); } catch (Exception ignored) { }
                    SupplierLookupResponseDTO resolvedSupplier = supplier;
                    WarehouseLookupResponseDTO resolvedWarehouse = warehouse;
                    return order.getLineItems().stream()
                            .map(item -> new PurchaseOrderReportRowResponse(
                                    order.getPoId(),
                                    order.getPoNumber(),
                                    normalizeStatus(order.getStatus()).name(),
                                    paymentSnapshot.getPaymentStatus(),
                                    paymentSnapshot.getPaymentNumber(),
                                    paymentSnapshot.getRazorpayOrderId(),
                                    paymentSnapshot.getRazorpayPaymentId(),
                                    paymentSnapshot.getPaymentAmount(),
                                    paymentSnapshot.getPaidAt(),
                                    order.getSupplierId(),
                                    resolvedSupplier != null ? resolvedSupplier.getName() : null,
                                    order.getWarehouseId(),
                                    resolvedWarehouse != null ? resolvedWarehouse.getName() : null,
                                    item.getProductId(),
                                    item.getProductSku(),
                                    item.getProductName(),
                                    null,
                                    item.getUnitCost(),
                                    defaultQty(item.getQuantity()),
                                    defaultQty(item.getReceivedQty()),
                                    pendingQuantity(item),
                                    item.getTotalCost(),
                                    defaultMoney(order.getTotalAmount()),
                                    order.getOrderDate(),
                                    order.getExpectedDate(),
                                    order.getApprovedBy(),
                                    order.getApprovedAt(),
                                    order.getCreatedAt()));
                })
                .filter(row -> paymentStatus == null || paymentStatus.isBlank()
                        || paymentStatus.equalsIgnoreCase(row.paymentStatus()))
                .toList();

        int start = Math.min(page * size, rows.size());
        int end = Math.min(start + size, rows.size());
        return new org.springframework.data.domain.PageImpl<>(
                rows.subList(start, end),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")),
                rows.size());
    }

    private PurchaseOrderSummaryResponse buildSummary(List<PurchaseOrder> orders) {
        BigDecimal totalValue = orders.stream().map(po -> defaultMoney(po.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingValue = orders.stream()
                .filter(po -> normalizeStatus(po.getStatus()) == POStatus.PENDING_APPROVAL)
                .map(po -> defaultMoney(po.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivedValue = orders.stream()
                .filter(po -> normalizeStatus(po.getStatus()) == POStatus.RECEIVED)
                .map(po -> defaultMoney(po.getTotalAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        long overdue = orders.stream().filter(this::isOverdue).count();
        return new PurchaseOrderSummaryResponse(
                orders.size(),
                countByStatus(orders, POStatus.DRAFT),
                countByStatus(orders, POStatus.PENDING_APPROVAL),
                countByStatus(orders, POStatus.APPROVED),
                countByStatus(orders, POStatus.PARTIALLY_RECEIVED),
                countByStatus(orders, POStatus.RECEIVED),
                countByStatus(orders, POStatus.CANCELLED),
                countByStatus(orders, POStatus.REJECTED),
                overdue,
                totalValue,
                pendingValue,
                receivedValue
        );
    }

    public PurchaseAnalyticsResponse getPurchaseAnalytics(LocalDate fromDate, LocalDate toDate) {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll();
        if (fromDate != null) {
            orders = orders.stream()
                    .filter(po -> po.getCreatedAt() != null && !po.getCreatedAt().toLocalDate().isBefore(fromDate))
                    .toList();
        }
        if (toDate != null) {
            orders = orders.stream()
                    .filter(po -> po.getCreatedAt() != null && !po.getCreatedAt().toLocalDate().isAfter(toDate))
                    .toList();
        }
        BigDecimal totalSpend = orders.stream()
                .filter(po -> normalizeStatus(po.getStatus()) == POStatus.RECEIVED)
                .map(po -> defaultMoney(po.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlySpend = orders.stream()
                .filter(po -> po.getCreatedAt() != null && YearMonth.from(po.getCreatedAt()).equals(YearMonth.now()))
                .map(po -> defaultMoney(po.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> topSuppliers = orders.stream()
                .collect(java.util.stream.Collectors.groupingBy(PurchaseOrder::getSupplierId, java.util.stream.Collectors.counting()))
                .entrySet().stream().sorted(Map.Entry.<Long, Long>comparingByValue().reversed()).limit(5)
                .map(entry -> "Supplier #" + entry.getKey()).toList();
        List<String> topProducts = orders.stream()
                .flatMap(po -> po.getLineItems().stream())
                .collect(java.util.stream.Collectors.groupingBy(POLineItem::getProductId, java.util.stream.Collectors.summingInt(POLineItem::getQuantity)))
                .entrySet().stream().sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()).limit(5)
                .map(entry -> "Product #" + entry.getKey()).toList();
        long avgApprovalTime = averageApprovalHours(orders);
        long avgDeliveryDelay = averageDeliveryDelayDays(orders);
        return new PurchaseAnalyticsResponse(
                totalSpend,
                monthlySpend,
                topSuppliers,
                topProducts,
                countByStatus(orders, POStatus.PENDING_APPROVAL),
                orders.stream().filter(this::isOverdue).count(),
                avgApprovalTime,
                avgDeliveryDelay
        );
    }

    public List<PurchaseOrderResponse> getOverduePurchaseOrders() {
        return purchaseOrderRepository.findOverduePurchaseOrders(OVERDUE_STATUSES, LocalDate.now()).stream().map(this::toResponse).toList();
    }

    public List<PurchaseOrderResponse> getPendingApprovalPurchaseOrders() {
        return purchaseOrderRepository.findByStatus(POStatus.PENDING_APPROVAL).stream().map(this::toResponse).toList();
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void publishOverduePurchaseOrders() {
        List<PurchaseOrder> overdueOrders = purchaseOrderRepository.findOverduePurchaseOrders(OVERDUE_STATUSES, LocalDate.now());
        overdueOrders.forEach(order -> publish(order, order.getStatus(), order.getStatus(), order.getCreatedById(), overdueRouting, "Purchase order overdue"));
        log.info("Overdue purchase order scan complete. Count={}", overdueOrders.size());
    }

    private void validateCreateOrUpdate(Long supplierId, Long warehouseId, List<CreatePurchaseOrderLineItemRequest> lineItems) {
        supplierGateway.ensureSupplierExists(supplierId);
        warehouseGateway.ensureWarehouseExists(warehouseId);
        if (lineItems == null || lineItems.isEmpty()) {
            throw new InvalidPOStateException("Line items are required");
        }
        Set<Long> products = new HashSet<>();
        for (CreatePurchaseOrderLineItemRequest lineItem : lineItems) {
            if (!products.add(lineItem.productId())) {
                throw new InvalidPOStateException("Duplicate product in the same purchase order");
            }
            StockProductThresholdDTO product = productGateway.getProductDetails(lineItem.productId());
            if (product == null || product.getProductId() == null || Boolean.FALSE.equals(product.getIsActive())) {
                throw new InvalidPOStateException("Product not found or inactive");
            }
        }
    }

    private void applyEditableFields(PurchaseOrder purchaseOrder, Long supplierId, Long warehouseId, LocalDate expectedDate,
            String paymentTerms, String notes, BigDecimal taxAmount, BigDecimal discountAmount, BigDecimal shippingAmount,
            List<CreatePurchaseOrderLineItemRequest> lineItems) {
        purchaseOrder.setSupplierId(supplierId);
        purchaseOrder.setWarehouseId(warehouseId);
        purchaseOrder.setExpectedDate(expectedDate);
        purchaseOrder.setPaymentTerms(paymentTerms);
        purchaseOrder.setNotes(notes);
        purchaseOrder.setTaxAmount(defaultMoney(taxAmount));
        purchaseOrder.setDiscountAmount(defaultMoney(discountAmount));
        purchaseOrder.setShippingAmount(defaultMoney(shippingAmount));
        purchaseOrder.getLineItems().clear();
        for (CreatePurchaseOrderLineItemRequest lineItemRequest : lineItems) {
            StockProductThresholdDTO product = productGateway.getProductDetails(lineItemRequest.productId());
            POLineItem lineItem = POLineItem.builder()
                    .productId(lineItemRequest.productId())
                    .productSku(product != null ? product.getSku() : null)
                    .productName(product != null ? product.getName() : null)
                    .quantity(lineItemRequest.orderedQuantity())
                    .receivedQty(0)
                    .unitCost(lineItemRequest.unitCost())
                    .totalCost(lineItemRequest.unitCost().multiply(BigDecimal.valueOf(lineItemRequest.orderedQuantity())))
                    .notes(lineItemRequest.notes())
                    .purchaseOrder(purchaseOrder)
                    .build();
            purchaseOrder.getLineItems().add(lineItem);
        }
        recalculateAmounts(purchaseOrder);
    }

    private void recalculateAmounts(PurchaseOrder purchaseOrder) {
        BigDecimal subtotal = purchaseOrder.getLineItems().stream()
                .map(POLineItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = subtotal
                .add(defaultMoney(purchaseOrder.getTaxAmount()))
                .add(defaultMoney(purchaseOrder.getShippingAmount()))
                .subtract(defaultMoney(purchaseOrder.getDiscountAmount()));
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPOStateException("Total amount cannot be negative");
        }
        purchaseOrder.setSubtotalAmount(subtotal);
        purchaseOrder.setTotalAmount(total);
    }

    private PurchaseOrder getEntity(Long poId) {
        return purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found with ID: " + poId));
    }

    private void ensureEditable(PurchaseOrder purchaseOrder) {
        POStatus status = normalizeStatus(purchaseOrder.getStatus());
        if (status != POStatus.DRAFT && status != POStatus.PENDING_APPROVAL) {
            throw new InvalidPOStateException("Only draft or pending approval purchase orders can be updated");
        }
    }

    private void ensureStatus(PurchaseOrder purchaseOrder, Set<POStatus> allowed, String action) {
        POStatus status = normalizeStatus(purchaseOrder.getStatus());
        if (!allowed.contains(status)) {
            throw new InvalidPOStateException("Cannot " + action + " purchase order in status " + status);
        }
    }

    private void ensureReceivableStatus(PurchaseOrder purchaseOrder) {
        POStatus status = normalizeStatus(purchaseOrder.getStatus());
        if (status == POStatus.PAID || status == POStatus.PARTIALLY_RECEIVED) {
            return;
        }
        throw new InvalidPurchaseOrderStatusException("Goods can be received only after payment is completed.");
    }

    private void saveHistory(Long poId, PurchaseOrderAction action, POStatus oldStatus, POStatus newStatus, Long actorId, String remarks) {
        historyRepository.save(PurchaseOrderHistory.builder()
                .purchaseOrderId(poId)
                .action(action.name())
                .oldStatus(oldStatus != null ? normalizeStatus(oldStatus).name() : null)
                .newStatus(newStatus != null ? normalizeStatus(newStatus).name() : null)
                .actorId(actorId)
                .remarks(remarks)
                .build());
    }

    private void publish(PurchaseOrder purchaseOrder, POStatus oldStatus, POStatus newStatus, Long actorId, String routingKey, String reason) {
        String normalizedStatus = normalizeStatus(purchaseOrder.getStatus()).name();
        purchaseEventPublisher.publish(routingKey, new PurchaseEvent(
                UUID.randomUUID().toString(),
                routingKey,
                purchaseOrder.getPoId(),
                purchaseOrder.getPoNumber(),
                purchaseOrder.getSupplierId(),
                purchaseOrder.getWarehouseId(),
                normalizedStatus,
                oldStatus != null ? normalizeStatus(oldStatus).name() : null,
                newStatus != null ? normalizeStatus(newStatus).name() : null,
                purchaseOrder.getTotalAmount(),
                actorId,
                LocalDateTime.now(),
                reason,
                purchaseOrder.getLineItems().stream()
                        .map(item -> new PurchaseEventLineItem(
                                item.getLineItemId(),
                                item.getProductId(),
                                item.getQuantity(),
                                item.getReceivedQty(),
                                defaultQty(item.getQuantity()) - defaultQty(item.getReceivedQty()),
                                item.getUnitCost()))
                        .toList(),
                null,
                Map.of("purchaseOrderId", purchaseOrder.getPoId(), "status", normalizedStatus)));
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder purchaseOrder) {
        SupplierLookupResponseDTO supplier = null;
        WarehouseLookupResponseDTO warehouse = null;
        List<POLineItem> lineItems = purchaseOrder.getLineItems() != null ? purchaseOrder.getLineItems() : List.of();
        POStatus normalizedStatus = normalizeStatus(purchaseOrder.getStatus());
        try { supplier = supplierGateway.getSupplier(purchaseOrder.getSupplierId()); } catch (Exception ignored) { }
        try { warehouse = warehouseGateway.getWarehouse(purchaseOrder.getWarehouseId()); } catch (Exception ignored) { }
        return new PurchaseOrderResponse(
                purchaseOrder.getPoId(),
                purchaseOrder.getPoNumber(),
                purchaseOrder.getSupplierId(),
                supplier != null ? supplier.getName() : null,
                purchaseOrder.getWarehouseId(),
                warehouse != null ? warehouse.getName() : null,
                purchaseOrder.getCreatedById(),
                null,
                purchaseOrder.getApprovedBy(),
                null,
                normalizedStatus != null ? normalizedStatus.name() : null,
                defaultMoney(purchaseOrder.getSubtotalAmount()),
                defaultMoney(purchaseOrder.getTaxAmount()),
                defaultMoney(purchaseOrder.getDiscountAmount()),
                defaultMoney(purchaseOrder.getShippingAmount()),
                defaultMoney(purchaseOrder.getTotalAmount()),
                purchaseOrder.getExpectedDate(),
                purchaseOrder.getActualDeliveryDate(),
                purchaseOrder.getPaymentTerms(),
                purchaseOrder.getNotes(),
                purchaseOrder.getApprovalRemarks(),
                purchaseOrder.getRejectionReason(),
                purchaseOrder.getCancellationReason(),
                purchaseOrder.getSubmittedAt(),
                purchaseOrder.getApprovedAt(),
                purchaseOrder.getRejectedAt(),
                purchaseOrder.getCancelledAt(),
                purchaseOrder.getReceivedAt(),
                purchaseOrder.getCreatedAt(),
                purchaseOrder.getUpdatedAt(),
                isOverdue(purchaseOrder),
                null,
                false,
                lineItems.stream().map(this::toLineItemResponse).toList(),
                historyRepository.findByPurchaseOrderIdOrderByActionAtAsc(purchaseOrder.getPoId()).stream().map(this::toHistoryResponse).toList());
    }

    private PurchaseOrderLineItemResponse toLineItemResponse(POLineItem item) {
        return new PurchaseOrderLineItemResponse(
                item.getLineItemId(),
                item.getProductId(),
                item.getProductSku(),
                item.getProductName(),
                item.getQuantity(),
                defaultQty(item.getReceivedQty()),
                defaultQty(item.getQuantity()) - defaultQty(item.getReceivedQty()),
                item.getUnitCost(),
                item.getTotalCost(),
                item.getNotes());
    }

    private PurchaseOrderHistoryResponse toHistoryResponse(PurchaseOrderHistory history) {
        return new PurchaseOrderHistoryResponse(
                history.getHistoryId(),
                history.getAction(),
                history.getOldStatus(),
                history.getNewStatus(),
                history.getActorId(),
                history.getRemarks(),
                history.getActionAt());
    }

    private Pageable pageable(int page, int size, String sortBy, String sortDir) {
        String resolvedSortBy = normalizeSortBy(sortBy);
        return PageRequest.of(page, size, Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, resolvedSortBy));
    }

    private String normalizeSortBy(String sortBy) {
        String requestedSortBy = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim();

        return switch (requestedSortBy) {
            case "purchaseOrderId" -> "poId";
            case "expectedDeliveryDate" -> "expectedDate";
            default -> {
                if (!ALLOWED_SORT_FIELDS.contains(requestedSortBy)) {
                    throw new IllegalArgumentException("Invalid purchase order sort field: " + requestedSortBy);
                }
                yield requestedSortBy;
            }
        };
    }

    private String generatePoNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int suffix = 1;
        String poNumber;
        do {
            poNumber = "PO-" + datePart + "-" + String.format("%04d", suffix++);
        } while (purchaseOrderRepository.existsByPoNumber(poNumber));
        return poNumber;
    }

    private boolean hasAnyReceivedQuantity(PurchaseOrder purchaseOrder) {
        return purchaseOrder.getLineItems().stream().anyMatch(item -> defaultQty(item.getReceivedQty()) > 0);
    }

    private boolean isOverdue(PurchaseOrder purchaseOrder) {
        return purchaseOrder.getExpectedDate() != null
                && purchaseOrder.getExpectedDate().isBefore(LocalDate.now())
                && normalizeStatus(purchaseOrder.getStatus()) != null
                && OVERDUE_STATUSES.contains(normalizeStatus(purchaseOrder.getStatus()));
    }

    private POStatus normalizeStatus(POStatus status) {
        if (status == POStatus.FULLY_RECEIVED) return POStatus.RECEIVED;
        return status;
    }

    private long countByStatus(List<PurchaseOrder> orders, POStatus status) {
        return orders.stream().filter(po -> normalizeStatus(po.getStatus()) == status).count();
    }

    private long averageApprovalHours(List<PurchaseOrder> orders) {
        return Math.round(orders.stream()
                .filter(po -> po.getSubmittedAt() != null && po.getApprovedAt() != null)
                .mapToLong(po -> ChronoUnit.HOURS.between(po.getSubmittedAt(), po.getApprovedAt()))
                .average().orElse(0));
    }

    private long averageDeliveryDelayDays(List<PurchaseOrder> orders) {
        return Math.round(orders.stream()
                .filter(po -> po.getExpectedDate() != null && po.getActualDeliveryDate() != null)
                .mapToLong(po -> ChronoUnit.DAYS.between(po.getExpectedDate(), po.getActualDeliveryDate()))
                .average().orElse(0));
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int defaultQty(Integer value) {
        return value == null ? 0 : value;
    }

    private int pendingQuantity(POLineItem item) {
        return defaultQty(item.getQuantity()) - defaultQty(item.getReceivedQty());
    }
}

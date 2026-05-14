package com.stockpro.purchaseservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stockpro.purchaseservice.entity.POLineItem;
import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.entity.PurchaseOrder;
import com.stockpro.purchaseservice.entity.PurchaseOrderHistory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PurchaseOrderRepositoryTest {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private POLineItemRepository poLineItemRepository;

    @Autowired
    private PurchaseOrderHistoryRepository historyRepository;

    @Test
    void purchaseOrderQueriesReturnExpectedRecords() {
        PurchaseOrder overdueApproved = saveOrder("PO-001", POStatus.APPROVED, LocalDate.now().minusDays(2), 1L, 2L, 2L);
        PurchaseOrder futureDraft = saveOrder("PO-002", POStatus.DRAFT, LocalDate.now().plusDays(3), 1L, 3L, 3L);
        PurchaseOrder fullyReceived = saveOrder("PO-003", POStatus.FULLY_RECEIVED, LocalDate.now().minusDays(1), 2L, 2L, 2L);

        assertTrue(purchaseOrderRepository.findByPurchaseOrderId(overdueApproved.getPoId()).isPresent());
        assertTrue(purchaseOrderRepository.existsByPoNumber("PO-002"));
        assertEquals(2, purchaseOrderRepository.findBySupplierId(1L).size());
        assertEquals(2, purchaseOrderRepository.findByWarehouseId(2L).size());
        assertEquals(1, purchaseOrderRepository.findAllByStatusIn(Set.of(POStatus.DRAFT)).size());
        assertEquals(2, purchaseOrderRepository.findOverduePurchaseOrders(
                EnumSet.of(POStatus.APPROVED, POStatus.FULLY_RECEIVED), LocalDate.now()).size());
        assertEquals(1, purchaseOrderRepository.findByStatusAndExpectedDateBefore(
                POStatus.APPROVED, LocalDate.now()).size());
        List<PurchaseOrder> createdByOrders = purchaseOrderRepository.findByCreatedById(3L);
        assertEquals(1, createdByOrders.size());
        assertEquals(futureDraft.getPoNumber(), createdByOrders.get(0).getPoNumber());
    }

    @Test
    void lineItemAndHistoryRepositoriesReturnAssociatedRecords() throws Exception {
        PurchaseOrder order = saveOrder("PO-100", POStatus.PAID, LocalDate.now().plusDays(1), 4L, 5L, 2L);
        POLineItem secondLine = POLineItem.builder()
                .productId(200L)
                .productSku("SKU-200")
                .productName("Product 200")
                .quantity(3)
                .receivedQty(1)
                .unitCost(BigDecimal.valueOf(15))
                .totalCost(BigDecimal.valueOf(45))
                .purchaseOrder(order)
                .build();
        poLineItemRepository.saveAndFlush(secondLine);

        PurchaseOrderHistory created = historyRepository.saveAndFlush(PurchaseOrderHistory.builder()
                .purchaseOrderId(order.getPoId())
                .action("CREATED")
                .newStatus("DRAFT")
                .actorId(4L)
                .remarks("created")
                .build());
        Thread.sleep(5);
        PurchaseOrderHistory submitted = historyRepository.saveAndFlush(PurchaseOrderHistory.builder()
                .purchaseOrderId(order.getPoId())
                .action("SUBMITTED")
                .oldStatus("DRAFT")
                .newStatus("PENDING_APPROVAL")
                .actorId(4L)
                .remarks("submitted")
                .build());

        assertEquals(2, poLineItemRepository.findByPurchaseOrderPoId(order.getPoId()).size());
        assertEquals(1, poLineItemRepository.findByProductId(200L).size());
        assertEquals(
                List.of(created.getHistoryId(), submitted.getHistoryId()),
                historyRepository.findByPurchaseOrderIdOrderByActionAtAsc(order.getPoId())
                        .stream()
                        .map(PurchaseOrderHistory::getHistoryId)
                        .toList());
    }

    private PurchaseOrder saveOrder(String poNumber, POStatus status, LocalDate expectedDate, Long supplierId, Long createdById, Long warehouseId) {
        PurchaseOrder order = new PurchaseOrder();
        order.setPoNumber(poNumber);
        order.setSupplierId(supplierId);
        order.setWarehouseId(warehouseId);
        order.setCreatedById(createdById);
        order.setStatus(status);
        order.setExpectedDate(expectedDate);
        order.setTotalAmount(BigDecimal.valueOf(100));
        order.setSubtotalAmount(BigDecimal.valueOf(100));
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingAmount(BigDecimal.ZERO);

        POLineItem lineItem = POLineItem.builder()
                .productId(100L)
                .productSku("SKU-100")
                .productName("Product 100")
                .quantity(5)
                .receivedQty(status == POStatus.FULLY_RECEIVED ? 5 : 0)
                .unitCost(BigDecimal.valueOf(20))
                .totalCost(BigDecimal.valueOf(100))
                .purchaseOrder(order)
                .build();

        order.setLineItems(new java.util.ArrayList<>(List.of(lineItem)));
        return purchaseOrderRepository.saveAndFlush(order);
    }
}

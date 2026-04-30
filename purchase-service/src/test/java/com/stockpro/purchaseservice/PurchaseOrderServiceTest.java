package com.stockpro.purchaseservice;

import com.stockpro.purchaseservice.dto.GoodsReceiptDTO;
import com.stockpro.purchaseservice.dto.POLineItemDTO;
import com.stockpro.purchaseservice.dto.PurchaseOrderRequestDTO;
import com.stockpro.purchaseservice.dto.PurchaseOrderResponseDTO;
import com.stockpro.purchaseservice.dto.StockProductThresholdDTO;
import com.stockpro.purchaseservice.entity.POLineItem;
import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.entity.PurchaseOrder;
import com.stockpro.purchaseservice.exception.InvalidPOStateException;
import com.stockpro.purchaseservice.exception.OverReceiptException;
import com.stockpro.purchaseservice.exception.PurchaseOrderNotFoundException;
import com.stockpro.purchaseservice.rabbitmq.POEventPublisher;
import com.stockpro.purchaseservice.repository.PurchaseOrderRepository;
import com.stockpro.purchaseservice.service.ProductCatalogGateway;
import com.stockpro.purchaseservice.service.PurchaseOrderMapper;
import com.stockpro.purchaseservice.service.PurchaseOrderService;
import com.stockpro.purchaseservice.service.PurchaseOrderValidationService;
import com.stockpro.purchaseservice.service.PurchaseOrderWorkflow;
import com.stockpro.purchaseservice.service.SupplierGateway;
import com.stockpro.purchaseservice.service.WarehouseGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierGateway supplierGateway;

    @Mock
    private WarehouseGateway warehouseGateway;

    @Mock
    private ProductCatalogGateway productCatalogGateway;

    @Mock
    private POEventPublisher poEventPublisher;

    private PurchaseOrderService purchaseOrderService;

    private PurchaseOrder purchaseOrder;
    private PurchaseOrderRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        purchaseOrderService = new PurchaseOrderService(
                purchaseOrderRepository,
                new PurchaseOrderMapper(),
                new PurchaseOrderValidationService(),
                new PurchaseOrderWorkflow(),
                supplierGateway,
                warehouseGateway,
                productCatalogGateway,
                poEventPublisher);

        POLineItem lineItem = POLineItem.builder()
                .lineItemId(1L)
                .productId(1L)
                .quantity(10)
                .unitCost(new BigDecimal("100.00"))
                .totalCost(new BigDecimal("1000.00"))
                .receivedQty(0)
                .build();

        purchaseOrder = PurchaseOrder.builder()
                .poId(1L)
                .supplierId(1L)
                .warehouseId(1L)
                .createdById(1L)
                .status(POStatus.DRAFT)
                .totalAmount(new BigDecimal("1000.00"))
                .orderDate(LocalDate.now())
                .expectedDate(LocalDate.now().plusDays(7))
                .lineItems(new ArrayList<>(List.of(lineItem)))
                .build();

        lineItem.setPurchaseOrder(purchaseOrder);

        POLineItemDTO lineItemDTO = new POLineItemDTO();
        lineItemDTO.setProductId(1L);
        lineItemDTO.setQuantity(10);
        lineItemDTO.setUnitCost(new BigDecimal("100.00"));

        requestDTO = new PurchaseOrderRequestDTO();
        requestDTO.setSupplierId(1L);
        requestDTO.setWarehouseId(1L);
        requestDTO.setCreatedById(1L);
        requestDTO.setLineItems(List.of(lineItemDTO));
        requestDTO.setExpectedDate(LocalDate.now().plusDays(7));
    }

    @Test
    void createPO_Success() {
        doNothing().when(supplierGateway).ensureSupplierExists(1L);
        doNothing().when(warehouseGateway).ensureWarehouseExists(1L);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);

        PurchaseOrderResponseDTO result = purchaseOrderService.createPO(requestDTO);

        assertNotNull(result);
        assertEquals(POStatus.DRAFT, result.getStatus());
        verify(supplierGateway).ensureSupplierExists(1L);
        verify(warehouseGateway).ensureWarehouseExists(1L);
        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
    }

    @Test
    void getPOById_Success() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));

        PurchaseOrderResponseDTO result = purchaseOrderService.getPOById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPoId());
    }

    @Test
    void getPOById_NotFound_ThrowsException() {
        when(purchaseOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PurchaseOrderNotFoundException.class,
                () -> purchaseOrderService.getPOById(99L));
    }

    @Test
    void getPOsByStatus_ReceivedAlias_ReturnsLegacyAndCurrentStatuses() {
        PurchaseOrder legacyReceived = PurchaseOrder.builder()
                .poId(2L)
                .status(POStatus.FULLY_RECEIVED)
                .lineItems(List.of())
                .build();
        when(purchaseOrderRepository.findAllByStatusIn(List.of(POStatus.RECEIVED, POStatus.FULLY_RECEIVED)))
                .thenReturn(List.of(purchaseOrder, legacyReceived));

        List<PurchaseOrderResponseDTO> result = purchaseOrderService.getPOsByStatus("RECEIVED");

        assertEquals(2, result.size());
    }

    @Test
    void getPOsByDateRange_InvalidRange_ThrowsException() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(1);

        assertThrows(IllegalArgumentException.class,
                () -> purchaseOrderService.getPOsByDateRange(startDate, endDate));
    }

    @Test
    void submitForApproval_Success() {
        purchaseOrder.setStatus(POStatus.DRAFT);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponseDTO result = purchaseOrderService.submitForApproval(1L);

        assertEquals(POStatus.PENDING, result.getStatus());
        verify(poEventPublisher).publishPOPending(anyLong(), anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void approvePO_Success() {
        purchaseOrder.setStatus(POStatus.PENDING);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponseDTO result = purchaseOrderService.approvePO(1L);

        assertEquals(POStatus.APPROVED, result.getStatus());
        verify(poEventPublisher).publishPOApproved(anyLong(), anyLong(), anyLong(), anyLong(), any(), any());
    }

    @Test
    void rejectPO_SetsCancelledStatus() {
        purchaseOrder.setStatus(POStatus.PENDING);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseOrderResponseDTO result = purchaseOrderService.rejectPO(1L, "Pricing mismatch");

        assertEquals(POStatus.CANCELLED, result.getStatus());
        assertEquals("REJECTED: Pricing mismatch", result.getNotes());
    }

    @Test
    void cancelPO_ApprovedStatus_ThrowsException() {
        purchaseOrder.setStatus(POStatus.APPROVED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));

        assertThrows(InvalidPOStateException.class,
                () -> purchaseOrderService.cancelPO(1L, "No longer needed"));
    }

    @Test
    void receiveGoods_FullReceipt_StatusReceived_AndWarehouseUpdated() {
        purchaseOrder.setStatus(POStatus.APPROVED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockProductThresholdDTO thresholds = new StockProductThresholdDTO();
        thresholds.setProductId(1L);
        thresholds.setReorderLevel(5);
        thresholds.setMaxStockLevel(20);
        when(productCatalogGateway.getProductThresholds(1L)).thenReturn(thresholds);

        GoodsReceiptDTO receipt = new GoodsReceiptDTO();
        receipt.setLineItemId(1L);
        receipt.setReceivedQty(10);

        PurchaseOrderResponseDTO result = purchaseOrderService.receiveGoods(1L, List.of(receipt));

        assertEquals(POStatus.RECEIVED, result.getStatus());
        verify(warehouseGateway).increaseStock(1L, 1L, 10, thresholds);
    }

    @Test
    void receiveGoods_ExceedsOrderedQty_ThrowsException_AndDoesNotCallWarehouse() {
        purchaseOrder.setStatus(POStatus.APPROVED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));

        GoodsReceiptDTO receipt = new GoodsReceiptDTO();
        receipt.setLineItemId(1L);
        receipt.setReceivedQty(11);

        assertThrows(OverReceiptException.class,
                () -> purchaseOrderService.receiveGoods(1L, List.of(receipt)));
        verify(warehouseGateway, never()).increaseStock(anyLong(), anyLong(), any(), any());
    }

    @Test
    void updatePO_DraftStatus_Success() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(supplierGateway).ensureSupplierExists(1L);
        doNothing().when(warehouseGateway).ensureWarehouseExists(1L);

        PurchaseOrderResponseDTO result = purchaseOrderService.updatePO(1L, requestDTO);

        assertNotNull(result);
        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
    }
}

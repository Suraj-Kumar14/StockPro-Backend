package com.stockpro.purchaseservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.stockpro.purchaseservice.dto.GoodsReceiptDTO;
import com.stockpro.purchaseservice.dto.POLineItemDTO;
import com.stockpro.purchaseservice.dto.PurchaseOrderRequestDTO;
import com.stockpro.purchaseservice.entity.POLineItem;
import com.stockpro.purchaseservice.exception.InvalidLineItemException;
import com.stockpro.purchaseservice.exception.InvalidPOStateException;
import com.stockpro.purchaseservice.exception.OverReceiptException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PurchaseOrderValidationServiceTest {

    private final PurchaseOrderValidationService service = new PurchaseOrderValidationService();

    @Test
    void validatePurchaseOrderRequestRejectsEmptyLineItems() {
        PurchaseOrderRequestDTO request = new PurchaseOrderRequestDTO();
        request.setLineItems(List.of());

        assertThrows(InvalidPOStateException.class, () -> service.validatePurchaseOrderRequest(request));
    }

    @Test
    void validatePurchaseOrderRequestRejectsDuplicateProducts() {
        PurchaseOrderRequestDTO request = new PurchaseOrderRequestDTO();
        request.setLineItems(List.of(lineItemDto(100L, 2, "10.00"), lineItemDto(100L, 1, "12.00")));

        assertThrows(InvalidLineItemException.class, () -> service.validatePurchaseOrderRequest(request));
    }

    @Test
    void validatePurchaseOrderRequestRejectsNegativeUnitCost() {
        PurchaseOrderRequestDTO request = new PurchaseOrderRequestDTO();
        request.setLineItems(List.of(lineItemDto(100L, 2, "-1.00")));

        assertThrows(InvalidLineItemException.class, () -> service.validatePurchaseOrderRequest(request));
    }

    @Test
    void validateGoodsReceiptsRejectsDuplicateLineItems() {
        List<GoodsReceiptDTO> receipts = List.of(goodsReceipt(1L, 2), goodsReceipt(1L, 1));

        assertThrows(InvalidLineItemException.class, () -> service.validateGoodsReceipts(receipts));
    }

    @Test
    void validateReceiptQuantityRejectsInvalidAndExcessiveQuantities() {
        POLineItem lineItem = POLineItem.builder()
                .productId(100L)
                .quantity(5)
                .receivedQty(2)
                .unitCost(BigDecimal.TEN)
                .totalCost(BigDecimal.valueOf(50))
                .build();

        assertThrows(InvalidLineItemException.class, () -> service.validateReceiptQuantity(lineItem, 0));
        assertThrows(OverReceiptException.class, () -> service.validateReceiptQuantity(lineItem, 4));
        assertDoesNotThrow(() -> service.validateReceiptQuantity(lineItem, 3));
    }

    private POLineItemDTO lineItemDto(Long productId, Integer quantity, String unitCost) {
        POLineItemDTO dto = new POLineItemDTO();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        dto.setUnitCost(new BigDecimal(unitCost));
        return dto;
    }

    private GoodsReceiptDTO goodsReceipt(Long lineItemId, Integer receivedQuantity) {
        GoodsReceiptDTO dto = new GoodsReceiptDTO();
        dto.setLineItemId(lineItemId);
        dto.setReceivedQty(receivedQuantity);
        return dto;
    }
}

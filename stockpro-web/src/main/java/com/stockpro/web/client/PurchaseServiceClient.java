package com.stockpro.web.client;

import com.stockpro.web.config.FeignSupportConfig;
import com.stockpro.web.dto.request.PurchaseLineItemRequest;
import com.stockpro.web.dto.request.PurchaseOrderRequest;
import com.stockpro.web.dto.response.PurchaseOrderResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "purchaseServiceClient",
        url = "${api.gateway.base-url}",
        configuration = FeignSupportConfig.class)
public interface PurchaseServiceClient {

    @PostMapping("/api/v1/purchase-orders")
    PurchaseOrderResponse createPurchaseOrder(@RequestBody PurchaseOrderRequest request);

    @PutMapping("/api/v1/purchase-orders/{poId}/approve")
    PurchaseOrderResponse approvePurchaseOrder(@PathVariable("poId") Long poId);

    @PostMapping("/api/v1/purchase-orders/{poId}/receive")
    PurchaseOrderResponse receiveGoods(@PathVariable("poId") Long poId,
            @RequestBody List<PurchaseLineItemRequest> lineItems);

    @PutMapping("/api/v1/purchase-orders/{poId}/cancel")
    PurchaseOrderResponse cancelPurchaseOrder(@PathVariable("poId") Long poId);

    @GetMapping("/api/v1/purchase-orders/status/{status}")
    List<PurchaseOrderResponse> getPurchaseOrdersByStatus(@PathVariable("status") String status);

    @GetMapping("/api/v1/purchase-orders/date-range")
    List<PurchaseOrderResponse> getPurchaseOrdersByDateRange(@RequestParam("start") LocalDate start,
            @RequestParam("end") LocalDate end);
}

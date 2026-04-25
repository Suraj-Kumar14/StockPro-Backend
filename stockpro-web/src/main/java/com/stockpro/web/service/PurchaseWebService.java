package com.stockpro.web.service;

import com.stockpro.web.dto.request.PurchaseLineItemRequest;
import com.stockpro.web.dto.request.PurchaseOrderFilterRequest;
import com.stockpro.web.dto.request.PurchaseOrderRequest;
import com.stockpro.web.dto.request.SupplierFormRequest;
import com.stockpro.web.dto.response.PurchaseOrderResponse;
import com.stockpro.web.dto.response.SupplierResponse;
import com.stockpro.web.dto.response.WarehouseResponse;
import com.stockpro.web.viewmodel.PageViewModel;
import com.stockpro.web.viewmodel.PurchaseOrderDetailViewModel;
import java.util.List;

public interface PurchaseWebService {

    PageViewModel<PurchaseOrderResponse> getPurchaseOrders(PurchaseOrderFilterRequest request);

    PurchaseOrderDetailViewModel getPurchaseOrderDetail(Long poId);

    PurchaseOrderRequest newPurchaseOrder(Long currentUserId);

    PurchaseOrderRequest getPurchaseOrderForm(Long poId);

    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request);

    PurchaseOrderResponse approvePurchaseOrder(Long poId);

    PurchaseOrderResponse cancelPurchaseOrder(Long poId);

    PurchaseOrderResponse receiveGoods(Long poId, List<PurchaseLineItemRequest> lineItems);

    List<SupplierResponse> getSuppliers(String search, String city, String country);

    SupplierResponse getSupplier(Long supplierId);

    SupplierFormRequest getSupplierForm(Long supplierId);

    SupplierResponse createSupplier(SupplierFormRequest request);

    SupplierResponse updateSupplier(Long supplierId, SupplierFormRequest request);

    SupplierResponse deactivateSupplier(Long supplierId);

    List<WarehouseResponse> getWarehouseOptions();
}

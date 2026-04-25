package com.stockpro.web.service.impl;

import com.stockpro.web.client.ProductServiceClient;
import com.stockpro.web.client.PurchaseServiceClient;
import com.stockpro.web.client.SupplierServiceClient;
import com.stockpro.web.client.WarehouseServiceClient;
import com.stockpro.web.dto.PurchaseOrderStatus;
import com.stockpro.web.dto.request.PurchaseLineItemRequest;
import com.stockpro.web.dto.request.PurchaseOrderFilterRequest;
import com.stockpro.web.dto.request.PurchaseOrderRequest;
import com.stockpro.web.dto.request.SupplierFormRequest;
import com.stockpro.web.dto.response.ProductResponse;
import com.stockpro.web.dto.response.PurchaseOrderResponse;
import com.stockpro.web.dto.response.SupplierResponse;
import com.stockpro.web.dto.response.WarehouseResponse;
import com.stockpro.web.service.PurchaseWebService;
import com.stockpro.web.viewmodel.PageViewModel;
import com.stockpro.web.viewmodel.PurchaseOrderDetailViewModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PurchaseWebServiceImpl implements PurchaseWebService {

    private final PurchaseServiceClient purchaseServiceClient;
    private final SupplierServiceClient supplierServiceClient;
    private final WarehouseServiceClient warehouseServiceClient;
    private final ProductServiceClient productServiceClient;

    public PurchaseWebServiceImpl(PurchaseServiceClient purchaseServiceClient,
            SupplierServiceClient supplierServiceClient,
            WarehouseServiceClient warehouseServiceClient,
            ProductServiceClient productServiceClient) {
        this.purchaseServiceClient = purchaseServiceClient;
        this.supplierServiceClient = supplierServiceClient;
        this.warehouseServiceClient = warehouseServiceClient;
        this.productServiceClient = productServiceClient;
    }

    @Override
    public PageViewModel<PurchaseOrderResponse> getPurchaseOrders(PurchaseOrderFilterRequest request) {
        List<PurchaseOrderResponse> orders = resolvePurchaseOrders(request).stream()
                .sorted(Comparator.comparing(PurchaseOrderResponse::getOrderDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return PageViewModel.fromList(orders, request.getPage(), request.getSize());
    }

    @Override
    public PurchaseOrderDetailViewModel getPurchaseOrderDetail(Long poId) {
        PurchaseOrderResponse purchaseOrder = getAllPurchaseOrders().stream()
                .filter(order -> Objects.equals(order.getPoId(), poId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + poId));

        SupplierResponse supplier = supplierServiceClient.getSupplierById(purchaseOrder.getSupplierId());
        WarehouseResponse warehouse = warehouseServiceClient.getWarehouseById(purchaseOrder.getWarehouseId());
        Map<Long, ProductResponse> productsById = new LinkedHashMap<>();
        purchaseOrder.getLineItems().forEach(lineItem -> productsById.computeIfAbsent(
                lineItem.getProductId(),
                productServiceClient::getProductById));

        boolean editSupported = purchaseOrder.getStatus() == PurchaseOrderStatus.DRAFT;
        return new PurchaseOrderDetailViewModel(purchaseOrder, supplier, warehouse, productsById, editSupported);
    }

    @Override
    public PurchaseOrderRequest newPurchaseOrder(Long currentUserId) {
        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setCreatedById(currentUserId);
        request.setLineItems(List.of(new PurchaseLineItemRequest()));
        return request;
    }

    @Override
    public PurchaseOrderRequest getPurchaseOrderForm(Long poId) {
        PurchaseOrderDetailViewModel detail = getPurchaseOrderDetail(poId);
        PurchaseOrderResponse order = detail.purchaseOrder();
        PurchaseOrderRequest request = new PurchaseOrderRequest();
        request.setPoId(order.getPoId());
        request.setSupplierId(order.getSupplierId());
        request.setWarehouseId(order.getWarehouseId());
        request.setCreatedById(order.getCreatedById());
        request.setStatus(order.getStatus());
        request.setTotalAmount(order.getTotalAmount());
        request.setOrderDate(order.getOrderDate());
        request.setExpectedDate(order.getExpectedDate());
        request.setReceivedDate(order.getReceivedDate());
        request.setNotes(order.getNotes());
        request.setReferenceNumber(order.getReferenceNumber());
        request.setLineItems(order.getLineItems().stream()
                .map(item -> new PurchaseLineItemRequest(
                        item.getLineItemId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitCost(),
                        item.getTotalCost(),
                        item.getReceivedQty()))
                .toList());
        return request;
    }

    @Override
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        request.setTotalAmount(calculateTotal(request.getLineItems()));
        return purchaseServiceClient.createPurchaseOrder(request);
    }

    @Override
    public PurchaseOrderResponse approvePurchaseOrder(Long poId) {
        return purchaseServiceClient.approvePurchaseOrder(poId);
    }

    @Override
    public PurchaseOrderResponse cancelPurchaseOrder(Long poId) {
        return purchaseServiceClient.cancelPurchaseOrder(poId);
    }

    @Override
    public PurchaseOrderResponse receiveGoods(Long poId, List<PurchaseLineItemRequest> lineItems) {
        return purchaseServiceClient.receiveGoods(poId, lineItems);
    }

    @Override
    public List<SupplierResponse> getSuppliers(String search, String city, String country) {
        List<SupplierResponse> suppliers;
        if (StringUtils.hasText(city)) {
            suppliers = supplierServiceClient.getSuppliersByCity(city);
        } else if (StringUtils.hasText(country)) {
            suppliers = supplierServiceClient.getSuppliersByCountry(country);
        } else if (StringUtils.hasText(search)) {
            suppliers = supplierServiceClient.searchSuppliers(search);
        } else {
            suppliers = supplierServiceClient.getAllSuppliers();
        }

        return suppliers.stream()
                .filter(supplier -> !StringUtils.hasText(search)
                        || supplier.getName().toLowerCase().contains(search.toLowerCase())
                        || supplier.getCity().toLowerCase().contains(search.toLowerCase())
                        || supplier.getCountry().toLowerCase().contains(search.toLowerCase()))
                .filter(supplier -> !StringUtils.hasText(city)
                        || supplier.getCity().equalsIgnoreCase(city))
                .filter(supplier -> !StringUtils.hasText(country)
                        || supplier.getCountry().equalsIgnoreCase(country))
                .sorted(Comparator.comparing(SupplierResponse::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public SupplierResponse getSupplier(Long supplierId) {
        return supplierServiceClient.getSupplierById(supplierId);
    }

    @Override
    public SupplierFormRequest getSupplierForm(Long supplierId) {
        if (supplierId == null) {
            return new SupplierFormRequest();
        }
        SupplierResponse supplier = supplierServiceClient.getSupplierById(supplierId);
        SupplierFormRequest form = new SupplierFormRequest();
        form.setSupplierId(supplier.getSupplierId());
        form.setName(supplier.getName());
        form.setContactPerson(supplier.getContactPerson());
        form.setEmail(supplier.getEmail());
        form.setPhone(supplier.getPhone());
        form.setAddress(supplier.getAddress());
        form.setCity(supplier.getCity());
        form.setCountry(supplier.getCountry());
        form.setTaxId(supplier.getTaxId());
        form.setPaymentTerms(supplier.getPaymentTerms());
        form.setLeadTimeDays(supplier.getLeadTimeDays());
        form.setRating(supplier.getRating());
        form.setActive(supplier.getActive());
        return form;
    }

    @Override
    public SupplierResponse createSupplier(SupplierFormRequest request) {
        return supplierServiceClient.createSupplier(request);
    }

    @Override
    public SupplierResponse updateSupplier(Long supplierId, SupplierFormRequest request) {
        return supplierServiceClient.updateSupplier(supplierId, request);
    }

    @Override
    public SupplierResponse deactivateSupplier(Long supplierId) {
        return supplierServiceClient.deactivateSupplier(supplierId);
    }

    @Override
    public List<WarehouseResponse> getWarehouseOptions() {
        return warehouseServiceClient.getWarehouses(0, 200, "name", "asc").getContent();
    }

    private List<PurchaseOrderResponse> resolvePurchaseOrders(PurchaseOrderFilterRequest request) {
        List<PurchaseOrderResponse> orders;
        if (request.getFromDate() != null && request.getToDate() != null) {
            orders = new ArrayList<>(purchaseServiceClient.getPurchaseOrdersByDateRange(
                    request.getFromDate(), request.getToDate()));
        } else if (request.getStatus() != null) {
            orders = new ArrayList<>(purchaseServiceClient.getPurchaseOrdersByStatus(request.getStatus().name()));
        } else {
            orders = new ArrayList<>(getAllPurchaseOrders());
        }

        return orders.stream()
                .filter(order -> request.getSupplierId() == null
                        || Objects.equals(order.getSupplierId(), request.getSupplierId()))
                .filter(order -> request.getStatus() == null
                        || order.getStatus() == request.getStatus())
                .filter(order -> request.getFromDate() == null
                        || Optional.ofNullable(order.getOrderDate()).orElse(request.getFromDate()).compareTo(request.getFromDate()) >= 0)
                .filter(order -> request.getToDate() == null
                        || Optional.ofNullable(order.getOrderDate()).orElse(request.getToDate()).compareTo(request.getToDate()) <= 0)
                .toList();
    }

    private List<PurchaseOrderResponse> getAllPurchaseOrders() {
        Map<Long, PurchaseOrderResponse> ordersById = new LinkedHashMap<>();
        for (PurchaseOrderStatus status : PurchaseOrderStatus.values()) {
            purchaseServiceClient.getPurchaseOrdersByStatus(status.name())
                    .forEach(order -> ordersById.putIfAbsent(order.getPoId(), order));
        }
        return new ArrayList<>(ordersById.values());
    }

    private BigDecimal calculateTotal(List<PurchaseLineItemRequest> lineItems) {
        return lineItems.stream()
                .map(line -> {
                    BigDecimal total = line.getUnitCost().multiply(BigDecimal.valueOf(line.getQuantity()));
                    line.setTotalCost(total);
                    return total;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

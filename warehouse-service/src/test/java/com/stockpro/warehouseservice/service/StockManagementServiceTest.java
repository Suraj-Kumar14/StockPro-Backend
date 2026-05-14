package com.stockpro.warehouseservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.warehouseservice.client.ProductCatalogClient;
import com.stockpro.warehouseservice.dto.ProductLookupResponseDTO;
import com.stockpro.warehouseservice.dto.request.AdjustStockRequest;
import com.stockpro.warehouseservice.dto.request.CreateStockLevelRequest;
import com.stockpro.warehouseservice.dto.request.ReleaseReservationRequest;
import com.stockpro.warehouseservice.dto.request.ReserveStockRequest;
import com.stockpro.warehouseservice.dto.request.StockIssueRequest;
import com.stockpro.warehouseservice.dto.request.StockReceiptRequest;
import com.stockpro.warehouseservice.dto.request.TransferStockRequest;
import com.stockpro.warehouseservice.dto.request.UpdateStockRequest;
import com.stockpro.warehouseservice.dto.response.StockLevelResponse;
import com.stockpro.warehouseservice.dto.response.StockSummaryResponse;
import com.stockpro.warehouseservice.dto.response.TransferStockResponse;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.enums.TransferStatus;
import com.stockpro.warehouseservice.events.StockEvent;
import com.stockpro.warehouseservice.exception.InvalidOperationException;
import com.stockpro.warehouseservice.exception.ProductLookupException;
import com.stockpro.warehouseservice.exception.StockLevelNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StockManagementServiceTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private InventoryOperationService inventoryOperationService;

    @Mock
    private WarehouseManagementService warehouseManagementService;

    @Mock
    private ProductCatalogClient productCatalogClient;

    @Mock
    private WarehouseEventPublisher warehouseEventPublisher;

    @InjectMocks
    private StockManagementService stockManagementService;

    private Warehouse activeWarehouse;
    private Warehouse inactiveWarehouse;
    private StockLevel stockLevel;
    private ProductLookupResponseDTO activeProduct;

    @BeforeEach
    void setUp() {
        activeWarehouse = Warehouse.builder()
                .warehouseId(1L)
                .name("Central Warehouse")
                .capacity(500)
                .usedCapacity(100)
                .isActive(true)
                .build();
        inactiveWarehouse = Warehouse.builder()
                .warehouseId(2L)
                .name("Inactive Warehouse")
                .capacity(500)
                .usedCapacity(0)
                .isActive(false)
                .build();
        stockLevel = StockLevel.builder()
                .stockId(10L)
                .warehouseId(1L)
                .productId(100L)
                .quantity(20)
                .reservedQuantity(5)
                .reorderLevel(10)
                .maxStockLevel(40)
                .binLocation("A-1")
                .build();
        activeProduct = ProductLookupResponseDTO.builder()
                .productId(100L)
                .name("Widget")
                .sku("W-100")
                .reorderLevel(10)
                .maxStockLevel(40)
                .isActive(true)
                .build();

        setRouting("stockCreatedRouting", "stock.created");
        setRouting("stockUpdatedRouting", "stock.updated");
        setRouting("stockReceivedRouting", "stock.received");
        setRouting("stockIssuedRouting", "stock.issued");
        setRouting("stockReservedRouting", "stock.reserved");
        setRouting("stockReleasedRouting", "stock.released");
        setRouting("stockTransferredRouting", "stock.transferred");
        setRouting("stockAdjustedRouting", "stock.adjusted");
        setRouting("stockLowRouting", "stock.low");
        setRouting("stockOverstockRouting", "stock.overstock");
    }

    @Test
    void createStockLevel_shouldPersistAndPublishCreationAndThresholdEvents() {
        CreateStockLevelRequest request = new CreateStockLevelRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setQuantity(12);
        request.setReservedQuantity(2);
        request.setLocationCode("A-01");

        StockLevel saved = StockLevel.builder()
                .stockId(99L)
                .warehouseId(1L)
                .productId(100L)
                .quantity(12)
                .reservedQuantity(2)
                .reorderLevel(10)
                .maxStockLevel(40)
                .binLocation("A-01")
                .build();

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);
        when(stockLevelRepository.existsByWarehouseIdAndProductId(1L, 100L)).thenReturn(false);
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(saved);
        when(inventoryOperationService.saveWarehouse(activeWarehouse)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);

        StockLevelResponse response = stockManagementService.createStockLevel(request, 50L);

        assertEquals(99L, response.stockId());
        assertEquals(112, activeWarehouse.getUsedCapacity());
        verify(inventoryOperationService).saveWarehouse(activeWarehouse);
        verify(warehouseEventPublisher).publishStockEvent(eq("stock.created"), any(StockEvent.class));
    }

    @Test
    void createStockLevel_shouldRejectDuplicateInactiveOrInactiveProduct() {
        CreateStockLevelRequest request = new CreateStockLevelRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setQuantity(5);

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);
        when(stockLevelRepository.existsByWarehouseIdAndProductId(1L, 100L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> stockManagementService.createStockLevel(request, 1L));

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(inactiveWarehouse);
        assertThrows(InvalidOperationException.class,
                () -> stockManagementService.createStockLevel(request, 1L));

        ProductLookupResponseDTO inactiveProduct = ProductLookupResponseDTO.builder()
                .productId(100L)
                .isActive(false)
                .build();
        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(inactiveProduct);

        assertThrows(ProductLookupException.class,
                () -> stockManagementService.createStockLevel(request, 1L));
    }

    @Test
    void getStockLookupsAndSearch_shouldMapResponses() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L)).thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.findByWarehouseId(1L, org.springframework.data.domain.PageRequest.of(
                0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastUpdated"))))
                .thenReturn(new PageImpl<>(List.of(stockLevel)));
        when(stockLevelRepository.findByProductId(100L, org.springframework.data.domain.PageRequest.of(
                0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "lastUpdated"))))
                .thenReturn(new PageImpl<>(List.of(stockLevel)));
        when(stockLevelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(stockLevel)));
        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);

        assertEquals(10L, stockManagementService.getStockLevel(1L, 100L).stockId());
        assertEquals(1, stockManagementService.getStockByWarehouse(1L, 0, 10).getTotalElements());
        assertEquals(1, stockManagementService.getStockByProduct(100L, 0, 10).getTotalElements());
        assertEquals(1, stockManagementService.searchStock(1L, 100L, "A-", true, true, 0, 10).getTotalElements());
    }

    @Test
    void updateStock_shouldRejectQuantityBelowReservedAndPublishThresholdEvents() {
        UpdateStockRequest request = new UpdateStockRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setQuantity(3);

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L)).thenReturn(Optional.of(stockLevel));

        assertThrows(InvalidOperationException.class,
                () -> stockManagementService.updateStock(request, 99L));

        request.setQuantity(40);
        when(stockLevelRepository.save(stockLevel)).thenReturn(stockLevel);
        when(inventoryOperationService.saveWarehouse(activeWarehouse)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);

        StockLevelResponse response = stockManagementService.updateStock(request, 99L);

        assertEquals(40, response.quantity());
        verify(warehouseEventPublisher).publishStockEvent(eq("stock.updated"), any(StockEvent.class));
        verify(warehouseEventPublisher).publishStockEvent(eq("stock.overstock"), any(StockEvent.class));
    }

    @Test
    void updateStock_shouldRejectUnknownProduct() {
        UpdateStockRequest request = new UpdateStockRequest();
        request.setWarehouseId(1L);
        request.setProductId(404L);
        request.setQuantity(8);

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(404L))
                .thenThrow(new ProductLookupException("Product not found with ID: 404"));

        assertThrows(ProductLookupException.class,
                () -> stockManagementService.updateStock(request, 8L));
    }

    @Test
    void receiveStock_shouldCreateRowWhenMissingAndUseFallbackReason() {
        StockReceiptRequest request = new StockReceiptRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setQuantity(8);

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(inventoryOperationService.resolveThresholds(100L, null, null))
                .thenReturn(new InventoryOperationService.ThresholdSettings(10, 40));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L)).thenReturn(Optional.empty());
        when(inventoryOperationService.handleReceipt(eq(activeWarehouse), any(StockLevel.class), eq(8), eq("Stock received")))
                .thenAnswer(invocation -> {
                    StockLevel created = invocation.getArgument(1);
                    created.setQuantity(8);
                    return new InventoryOperationService.StockMutation("RECEIPT", 8, 0, 8, "Stock received");
                });
        when(inventoryOperationService.saveStockLevel(any(StockLevel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryOperationService.saveWarehouse(activeWarehouse)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);

        StockLevelResponse response = stockManagementService.receiveStock(request, 12L);

        assertEquals(8, response.quantity());
        verify(inventoryOperationService).handleReceipt(eq(activeWarehouse), any(StockLevel.class), eq(8), eq("Stock received"));
        verify(warehouseEventPublisher).publishStockEvent(eq("stock.received"), any(StockEvent.class));
    }

    @Test
    void issueReserveReleaseAndAdjust_shouldDelegateAndPublishEvents() {
        StockIssueRequest issueRequest = new StockIssueRequest();
        issueRequest.setWarehouseId(1L);
        issueRequest.setProductId(100L);
        issueRequest.setQuantity(4);
        issueRequest.setReason("Issue");

        ReserveStockRequest reserveRequest = new ReserveStockRequest();
        reserveRequest.setWarehouseId(1L);
        reserveRequest.setProductId(100L);
        reserveRequest.setQuantity(3);
        reserveRequest.setReason("Reserve");

        ReleaseReservationRequest releaseRequest = new ReleaseReservationRequest();
        releaseRequest.setWarehouseId(1L);
        releaseRequest.setProductId(100L);
        releaseRequest.setQuantity(2);
        releaseRequest.setReason("Release");

        AdjustStockRequest adjustRequest = new AdjustStockRequest();
        adjustRequest.setWarehouseId(1L);
        adjustRequest.setProductId(100L);
        adjustRequest.setNewQuantity(25);
        adjustRequest.setReason("Adjust");

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L)).thenReturn(Optional.of(stockLevel));
        when(inventoryOperationService.saveStockLevel(stockLevel)).thenReturn(stockLevel);
        when(inventoryOperationService.saveWarehouse(activeWarehouse)).thenReturn(activeWarehouse);
        when(stockLevelRepository.save(stockLevel)).thenReturn(stockLevel);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);
        when(inventoryOperationService.handleIssue(activeWarehouse, stockLevel, 4, "Issue"))
                .thenAnswer(invocation -> {
                    stockLevel.setQuantity(16);
                    return new InventoryOperationService.StockMutation("ISSUE", 4, 20, 16, "Issue");
                });
        when(inventoryOperationService.reserveStock(stockLevel, 3))
                .thenAnswer(invocation -> {
                    stockLevel.setReservedQuantity(8);
                    return new InventoryOperationService.ReservationMutation(3, 5, 8);
                });
        when(inventoryOperationService.releaseReservation(stockLevel, 2))
                .thenAnswer(invocation -> {
                    stockLevel.setReservedQuantity(6);
                    return new InventoryOperationService.ReservationMutation(2, 8, 6);
                });

        stockManagementService.issueStock(issueRequest, 4L);
        stockManagementService.reserveStock(reserveRequest, 4L);
        stockManagementService.releaseReservation(releaseRequest, 4L);
        stockManagementService.adjustStock(adjustRequest, 4L);

        verify(inventoryOperationService).handleIssue(activeWarehouse, stockLevel, 4, "Issue");
        verify(inventoryOperationService).reserveStock(stockLevel, 3);
        verify(inventoryOperationService).releaseReservation(stockLevel, 2);
        verify(warehouseEventPublisher).publishStockEvent(eq("stock.issued"), any(StockEvent.class));
        verify(warehouseEventPublisher).publishStockEvent(eq("stock.reserved"), any(StockEvent.class));
        verify(warehouseEventPublisher).publishStockEvent(eq("stock.released"), any(StockEvent.class));
        verify(warehouseEventPublisher).publishStockEvent(eq("stock.adjusted"), any(StockEvent.class));
    }

    @Test
    void adjustStock_shouldRejectBelowReservedQuantity() {
        AdjustStockRequest request = new AdjustStockRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setNewQuantity(4);

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L)).thenReturn(Optional.of(stockLevel));

        assertThrows(InvalidOperationException.class,
                () -> stockManagementService.adjustStock(request, 1L));
    }

    @Test
    void transferStock_shouldRejectSameWarehouseAndSupportNewDestinationRow() {
        TransferStockRequest request = new TransferStockRequest();
        request.setSourceWarehouseId(1L);
        request.setDestinationWarehouseId(1L);
        request.setProductId(100L);
        request.setQuantity(5);

        assertThrows(InvalidOperationException.class,
                () -> stockManagementService.transferStock(request, 7L));

        request.setDestinationWarehouseId(3L);
        Warehouse destinationWarehouse = Warehouse.builder()
                .warehouseId(3L)
                .name("Destination")
                .capacity(400)
                .usedCapacity(50)
                .isActive(true)
                .build();

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(warehouseManagementService.getWarehouseEntity(3L)).thenReturn(destinationWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L)).thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.findByWarehouseIdAndProductId(3L, 100L)).thenReturn(Optional.empty());
        when(inventoryOperationService.saveStockLevel(any(StockLevel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryOperationService.saveWarehouse(activeWarehouse)).thenReturn(activeWarehouse);
        when(inventoryOperationService.saveWarehouse(destinationWarehouse)).thenReturn(destinationWarehouse);

        TransferStockResponse response = stockManagementService.transferStock(request, 7L);

        assertEquals(TransferStatus.COMPLETED, response.status());
        verify(warehouseEventPublisher).publishStockEvent(eq("stock.transferred"), any(StockEvent.class));
    }

    @Test
    void transferStock_shouldThrowStockLevelNotFoundWhenSourceRowMissing() {
        TransferStockRequest request = new TransferStockRequest();
        request.setSourceWarehouseId(1L);
        request.setDestinationWarehouseId(3L);
        request.setProductId(100L);
        request.setQuantity(5);

        Warehouse destinationWarehouse = Warehouse.builder()
                .warehouseId(3L)
                .name("Destination")
                .capacity(400)
                .usedCapacity(50)
                .isActive(true)
                .build();

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(warehouseManagementService.getWarehouseEntity(3L)).thenReturn(destinationWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 100L)).thenReturn(Optional.empty());

        assertThrows(StockLevelNotFoundException.class,
                () -> stockManagementService.transferStock(request, 7L));
    }

    @Test
    void getSummaryLowAndOverstock_shouldCalculateCounts() {
        StockLevel low = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(1L)
                .quantity(5)
                .reservedQuantity(1)
                .reorderLevel(5)
                .maxStockLevel(20)
                .build();
        StockLevel over = StockLevel.builder()
                .stockId(2L)
                .warehouseId(1L)
                .productId(2L)
                .quantity(25)
                .reservedQuantity(0)
                .reorderLevel(4)
                .maxStockLevel(20)
                .build();
        when(stockLevelRepository.findAll()).thenReturn(List.of(low, over));
        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(1L)).thenReturn(activeProduct);
        when(productCatalogClient.getProductById(2L))
                .thenThrow(new RuntimeException("product service down"));

        StockSummaryResponse summary = stockManagementService.getStockSummary();
        List<StockLevelResponse> lowItems = stockManagementService.getLowStockItems();
        List<StockLevelResponse> overItems = stockManagementService.getOverstockItems();

        assertEquals(2, summary.totalStockItems());
        assertEquals(1, summary.lowStockItemsCount());
        assertEquals(1, summary.overstockItemsCount());
        assertEquals(1, lowItems.size());
        assertEquals(1, overItems.size());
        assertNull(overItems.get(0).productName());
    }

    @Test
    void getStockLevel_shouldThrowWhenStockMissing() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 404L)).thenReturn(Optional.empty());

        assertThrows(StockLevelNotFoundException.class,
                () -> stockManagementService.getStockLevel(1L, 404L));
        verify(productCatalogClient, never()).getProductById(any());
    }

    @Test
    void createAndUpdateEvents_shouldCaptureExpectedRoutingKeys() {
        CreateStockLevelRequest request = new CreateStockLevelRequest();
        request.setWarehouseId(1L);
        request.setProductId(100L);
        request.setQuantity(10);
        request.setReservedQuantity(0);

        when(warehouseManagementService.getWarehouseEntity(1L)).thenReturn(activeWarehouse);
        when(productCatalogClient.getProductById(100L)).thenReturn(activeProduct);
        when(stockLevelRepository.existsByWarehouseIdAndProductId(1L, 100L)).thenReturn(false);
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(stockLevel);
        when(inventoryOperationService.saveWarehouse(activeWarehouse)).thenReturn(activeWarehouse);

        stockManagementService.createStockLevel(request, 1L);

        ArgumentCaptor<StockEvent> captor = ArgumentCaptor.forClass(StockEvent.class);
        verify(warehouseEventPublisher).publishStockEvent(eq("stock.created"), captor.capture());
        assertNotNull(captor.getValue().eventId());
        assertEquals("STOCK_LEVEL_CREATED", captor.getValue().eventType());
    }

    private void setRouting(String fieldName, String value) {
        ReflectionTestUtils.setField(stockManagementService, fieldName, value);
    }
}

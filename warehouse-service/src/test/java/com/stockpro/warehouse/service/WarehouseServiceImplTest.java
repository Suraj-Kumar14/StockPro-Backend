package com.stockpro.warehouse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.warehouse.client.MovementServiceClient;
import com.stockpro.warehouse.client.ProductServiceClient;
import com.stockpro.warehouse.dto.request.ReserveStockRequest;
import com.stockpro.warehouse.dto.request.TransferStockRequest;
import com.stockpro.warehouse.dto.request.UpdateStockRequest;
import com.stockpro.warehouse.dto.response.ProductSummaryResponse;
import com.stockpro.warehouse.dto.response.StockLevelResponse;
import com.stockpro.warehouse.dto.response.TransferStockResponse;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.exception.InsufficientStockException;
import com.stockpro.warehouse.mapper.StockLevelMapper;
import com.stockpro.warehouse.mapper.WarehouseMapper;
import com.stockpro.warehouse.repository.StockLevelRepository;
import com.stockpro.warehouse.repository.WarehouseRepository;
import com.stockpro.warehouse.security.AuthenticatedUser;
import com.stockpro.warehouse.security.SecurityUtils;
import com.stockpro.warehouse.service.impl.WarehouseServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private MovementServiceClient movementServiceClient;

    @Mock
    private SecurityUtils securityUtils;

    private WarehouseServiceImpl warehouseService;

    @BeforeEach
    void setUp() {
        warehouseService = new WarehouseServiceImpl(
                warehouseRepository,
                stockLevelRepository,
                new WarehouseMapper(),
                new StockLevelMapper(),
                productServiceClient,
                movementServiceClient,
                securityUtils,
                true,
                "WAREHOUSE-SERVICE");
    }

    @Test
    void updateStockShouldCreateNewStockAndRecordMovement() {
        Warehouse warehouse = activeWarehouse(1L, 100);
        UpdateStockRequest request = UpdateStockRequest.builder()
                .warehouseId(1L)
                .productId(501L)
                .quantity(new BigDecimal("25.0000"))
                .unitCost(new BigDecimal("149.5000"))
                .referenceId(9001L)
                .referenceType("PURCHASE_ORDER")
                .location("A-01")
                .notes("Goods received")
                .build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L)).thenReturn(Optional.empty());
        when(stockLevelRepository.sumQuantityByWarehouseId(1L)).thenReturn(BigDecimal.ZERO);
        when(productServiceClient.getProductById(501L)).thenReturn(activeProduct(501L));
        when(securityUtils.getCurrentUser()).thenReturn(authenticatedUser());
        when(stockLevelRepository.saveAndFlush(any(StockLevel.class))).thenAnswer(invocation -> {
            StockLevel stockLevel = invocation.getArgument(0);
            stockLevel.setStockId(10L);
            stockLevel.setLastUpdated(LocalDateTime.of(2026, 4, 24, 10, 0));
            return stockLevel;
        });
        when(warehouseRepository.saveAndFlush(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(movementServiceClient).recordMovement(any(), eq("WAREHOUSE-SERVICE"));

        StockLevelResponse response = warehouseService.updateStock(request);

        assertThat(response.getStockId()).isEqualTo(10L);
        assertThat(response.getQuantity()).isEqualByComparingTo("25.0000");
        assertThat(response.getAvailableQuantity()).isEqualByComparingTo("25.0000");
        verify(movementServiceClient).recordMovement(any(), eq("WAREHOUSE-SERVICE"));
    }

    @Test
    void reserveStockShouldRejectWhenAvailableQuantityIsInsufficient() {
        Warehouse warehouse = activeWarehouse(1L, 100);
        StockLevel stockLevel = StockLevel.builder()
                .stockId(10L)
                .warehouseId(1L)
                .productId(501L)
                .quantity(new BigDecimal("5.0000"))
                .reservedQuantity(new BigDecimal("1.0000"))
                .location("A-01")
                .build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L)).thenReturn(Optional.of(stockLevel));

        assertThatThrownBy(() -> warehouseService.reserveStock(ReserveStockRequest.builder()
                .warehouseId(1L)
                .productId(501L)
                .quantity(new BigDecimal("10.0000"))
                .referenceId(12001L)
                .referenceType("SALES_ORDER")
                .build()))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient available stock");
    }

    @Test
    void transferStockShouldMoveBalancesAndRecordTwoMovements() {
        Warehouse sourceWarehouse = activeWarehouse(1L, 100);
        Warehouse destinationWarehouse = activeWarehouse(2L, 100);
        StockLevel sourceStock = StockLevel.builder()
                .stockId(10L)
                .warehouseId(1L)
                .productId(501L)
                .quantity(new BigDecimal("30.0000"))
                .reservedQuantity(new BigDecimal("5.0000"))
                .location("A-01")
                .build();
        StockLevel destinationStock = StockLevel.builder()
                .stockId(11L)
                .warehouseId(2L)
                .productId(501L)
                .quantity(new BigDecimal("10.0000"))
                .reservedQuantity(new BigDecimal("0.0000"))
                .location("B-01")
                .build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(sourceWarehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(destinationWarehouse));
        when(productServiceClient.getProductById(501L)).thenReturn(activeProduct(501L));
        when(securityUtils.getCurrentUser()).thenReturn(authenticatedUser());
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 501L)).thenReturn(Optional.of(sourceStock));
        when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 501L)).thenReturn(Optional.of(destinationStock));
        when(stockLevelRepository.sumQuantityByWarehouseId(1L)).thenReturn(new BigDecimal("30.0000"));
        when(stockLevelRepository.sumQuantityByWarehouseId(2L)).thenReturn(new BigDecimal("10.0000"));
        when(stockLevelRepository.saveAndFlush(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(warehouseRepository.saveAndFlush(any(Warehouse.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(movementServiceClient).recordMovement(any(), eq("WAREHOUSE-SERVICE"));

        TransferStockResponse response = warehouseService.transferStock(TransferStockRequest.builder()
                .sourceWarehouseId(1L)
                .destinationWarehouseId(2L)
                .productId(501L)
                .quantity(new BigDecimal("12.0000"))
                .referenceId(15001L)
                .referenceType("WAREHOUSE_TRANSFER")
                .unitCost(new BigDecimal("149.5000"))
                .build());

        assertThat(response.getSourceBalance()).isEqualByComparingTo("18.0000");
        assertThat(response.getDestinationBalance()).isEqualByComparingTo("22.0000");
        verify(movementServiceClient, times(2)).recordMovement(any(), eq("WAREHOUSE-SERVICE"));
    }

    private Warehouse activeWarehouse(Long warehouseId, int capacity) {
        return Warehouse.builder()
                .warehouseId(warehouseId)
                .name("Warehouse " + warehouseId)
                .location("City")
                .address("Address")
                .managerId(1L)
                .capacity(capacity)
                .usedCapacity(0)
                .isActive(Boolean.TRUE)
                .build();
    }

    private ProductSummaryResponse activeProduct(Long productId) {
        return ProductSummaryResponse.builder()
                .productId(productId)
                .sku("SKU-" + productId)
                .name("Product " + productId)
                .reorderLevel(10)
                .maxStockLevel(100)
                .isActive(Boolean.TRUE)
                .build();
    }

    private AuthenticatedUser authenticatedUser() {
        return new AuthenticatedUser(
                99L,
                "inventory.manager@stockpro.com",
                "INVENTORY_MANAGER",
                List.of(new SimpleGrantedAuthority("ROLE_INVENTORY_MANAGER")));
    }
}

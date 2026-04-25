package com.stockpro.warehouse.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.warehouse.client.MovementServiceClient;
import com.stockpro.warehouse.client.ProductServiceClient;
import com.stockpro.warehouse.dto.request.RecordMovementRequest;
import com.stockpro.warehouse.dto.request.ReleaseReservationRequest;
import com.stockpro.warehouse.dto.request.ReserveStockRequest;
import com.stockpro.warehouse.dto.request.TransferStockRequest;
import com.stockpro.warehouse.dto.request.UpdateStockRequest;
import com.stockpro.warehouse.dto.response.ProductSummaryResponse;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.repository.StockLevelRepository;
import com.stockpro.warehouse.repository.WarehouseRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WarehouseServiceIntegrationTest {

    private static final String JWT_SECRET =
            "ThisIsASecretKeyForJwtTokenGenerationThatMustBeAtLeast32BytesLong12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @MockBean
    private ProductServiceClient productServiceClient;

    @MockBean
    private MovementServiceClient movementServiceClient;

    @BeforeEach
    void setUp() {
        stockLevelRepository.deleteAll();
        warehouseRepository.deleteAll();
        when(productServiceClient.getProductById(501L)).thenReturn(ProductSummaryResponse.builder()
                .productId(501L)
                .sku("SKU-501")
                .name("Industrial Drill")
                .reorderLevel(new BigDecimal("10.0000"))
                .maxStockLevel(new BigDecimal("100.0000"))
                .isActive(Boolean.TRUE)
                .build());
    }

    @Test
    void createWarehouseShouldPersistWarehouse() throws Exception {
        mockMvc.perform(post("/api/v1/warehouses")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L, "admin@stockpro.com", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Central Distribution Hub",
                                  "location": "Bengaluru",
                                  "address": "Plot 12, Electronics City",
                                  "managerId": 101,
                                  "capacity": 5000,
                                  "phone": "+91-9876543210"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warehouseId").isNumber())
                .andExpect(jsonPath("$.isActive").value(true));

        assertThat(warehouseRepository.findAll()).hasSize(1);
        assertThat(warehouseRepository.findAll().get(0).getName()).isEqualTo("Central Distribution Hub");
    }

    @Test
    void reserveAndReleaseStockShouldUpdateReservationBalances() throws Exception {
        Warehouse warehouse = warehouseRepository.save(activeWarehouse("Reserve Hub", 100));
        stockLevelRepository.save(StockLevel.builder()
                .warehouseId(warehouse.getWarehouseId())
                .productId(501L)
                .quantity(new BigDecimal("20.0000"))
                .reservedQuantity(new BigDecimal("0.0000"))
                .location("A-01")
                .build());

        mockMvc.perform(post("/api/v1/stock/reserve")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(2L, "staff@stockpro.com", "WAREHOUSE_STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ReserveStockRequest.builder()
                                .warehouseId(warehouse.getWarehouseId())
                                .productId(501L)
                                .quantity(new BigDecimal("5.0000"))
                                .referenceId(12001L)
                                .referenceType("SALES_ORDER")
                                .notes("Reserve")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(5.0))
                .andExpect(jsonPath("$.availableQuantity").value(15.0));

        mockMvc.perform(post("/api/v1/stock/release")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(2L, "staff@stockpro.com", "WAREHOUSE_STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ReleaseReservationRequest.builder()
                                .warehouseId(warehouse.getWarehouseId())
                                .productId(501L)
                                .quantity(new BigDecimal("2.0000"))
                                .referenceId(12001L)
                                .referenceType("SALES_ORDER")
                                .notes("Release")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(3.0))
                .andExpect(jsonPath("$.availableQuantity").value(17.0));

        StockLevel persisted = stockLevelRepository.findByWarehouseIdAndProductId(warehouse.getWarehouseId(), 501L).orElseThrow();
        assertThat(persisted.getReservedQuantity()).isEqualByComparingTo("3.0000");
    }

    @Test
    void transferStockShouldUpdateSourceAndDestinationAtomically() throws Exception {
        Warehouse sourceWarehouse = warehouseRepository.save(activeWarehouse("Source Hub", 100));
        Warehouse destinationWarehouse = warehouseRepository.save(activeWarehouse("Destination Hub", 100));
        stockLevelRepository.save(StockLevel.builder()
                .warehouseId(sourceWarehouse.getWarehouseId())
                .productId(501L)
                .quantity(new BigDecimal("30.0000"))
                .reservedQuantity(new BigDecimal("5.0000"))
                .location("A-01")
                .build());
        stockLevelRepository.save(StockLevel.builder()
                .warehouseId(destinationWarehouse.getWarehouseId())
                .productId(501L)
                .quantity(new BigDecimal("10.0000"))
                .reservedQuantity(new BigDecimal("0.0000"))
                .location("B-01")
                .build());

        mockMvc.perform(post("/api/v1/stock/transfer")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(3L, "manager@stockpro.com", "INVENTORY_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TransferStockRequest.builder()
                                .sourceWarehouseId(sourceWarehouse.getWarehouseId())
                                .destinationWarehouseId(destinationWarehouse.getWarehouseId())
                                .productId(501L)
                                .quantity(new BigDecimal("12.0000"))
                                .referenceId(15001L)
                                .referenceType("WAREHOUSE_TRANSFER")
                                .unitCost(new BigDecimal("149.5000"))
                                .notes("Transfer")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceBalance").value(18.0))
                .andExpect(jsonPath("$.destinationBalance").value(22.0));

        assertThat(stockLevelRepository.findByWarehouseIdAndProductId(sourceWarehouse.getWarehouseId(), 501L).orElseThrow()
                .getQuantity()).isEqualByComparingTo("18.0000");
        assertThat(stockLevelRepository.findByWarehouseIdAndProductId(destinationWarehouse.getWarehouseId(), 501L).orElseThrow()
                .getQuantity()).isEqualByComparingTo("22.0000");
    }

    @Test
    void updateStockShouldCallMovementServiceWithCorrectPayload() throws Exception {
        Warehouse warehouse = warehouseRepository.save(activeWarehouse("Movement Hub", 100));

        mockMvc.perform(put("/api/v1/stock/update")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(4L, "inventory@stockpro.com", "INVENTORY_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateStockRequest.builder()
                                .warehouseId(warehouse.getWarehouseId())
                                .productId(501L)
                                .quantity(new BigDecimal("25.0000"))
                                .unitCost(new BigDecimal("149.5000"))
                                .referenceId(9001L)
                                .referenceType("PURCHASE_ORDER")
                                .location("A-01")
                                .notes("Goods received")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(25.0));

        verify(movementServiceClient).recordMovement(any(RecordMovementRequest.class), eq("WAREHOUSE-SERVICE"));
        verify(movementServiceClient).recordMovement(org.mockito.ArgumentMatchers.argThat(request ->
                        request.getProductId().equals(501L)
                                && request.getWarehouseId().equals(warehouse.getWarehouseId())
                                && "STOCK_IN".equals(request.getMovementType())
                                && request.getPerformedBy().equals(4L)
                                && request.getBalanceAfter().compareTo(new BigDecimal("25.0000")) == 0),
                eq("WAREHOUSE-SERVICE"));
    }

    private Warehouse activeWarehouse(String name, int capacity) {
        return Warehouse.builder()
                .name(name)
                .location("Bengaluru")
                .address("Electronics City")
                .managerId(101L)
                .capacity(capacity)
                .usedCapacity(0)
                .isActive(Boolean.TRUE)
                .phone("+91-9876543210")
                .build();
    }

    private String bearerToken(Long userId, String email, String role) {
        return "Bearer " + Jwts.builder()
                .setClaims(Map.of("userId", userId, "role", role))
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}

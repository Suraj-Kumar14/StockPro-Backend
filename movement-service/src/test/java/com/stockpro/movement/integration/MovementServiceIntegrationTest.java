package com.stockpro.movement.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.movement.client.ProductServiceClient;
import com.stockpro.movement.dto.request.RecordMovementRequest;
import com.stockpro.movement.dto.response.ProductSummaryResponse;
import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.enums.MovementType;
import com.stockpro.movement.repository.MovementRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MovementServiceIntegrationTest {

    private static final String JWT_SECRET =
            "ThisIsASecretKeyForJwtTokenGenerationThatMustBeAtLeast32BytesLong12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MovementRepository movementRepository;

    @MockBean
    private ProductServiceClient productServiceClient;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        when(productServiceClient.getProductById(anyLong())).thenReturn(ProductSummaryResponse.builder()
                .productId(101L)
                .name("Test Product")
                .sku("SKU-101")
                .isActive(Boolean.TRUE)
                .build());
    }

    @Test
    void recordMovementShouldPersistMovement() throws Exception {
        RecordMovementRequest request = RecordMovementRequest.builder()
                .productId(101L)
                .warehouseId(5L)
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("25.0000"))
                .referenceId(9001L)
                .referenceType("PURCHASE_ORDER")
                .unitCost(new BigDecimal("149.5000"))
                .performedBy(12L)
                .notes("Goods received against PO-9001")
                .movementDate(LocalDateTime.of(2026, 4, 23, 10, 30))
                .balanceAfter(new BigDecimal("250.0000"))
                .build();

        mockMvc.perform(post("/api/v1/movements")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(12L, "warehouse.staff@stockpro.com", "WAREHOUSE_STAFF"))
                        .header("X-Source-Service", "WAREHOUSE-SERVICE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movementType").value("STOCK_IN"))
                .andExpect(jsonPath("$.performedBy").value(12));

        StockMovement persisted = movementRepository.findAll().get(0);
        assertThat(persisted.getPerformedBy()).isEqualTo(12L);
        assertThat(persisted.getSourceService()).isEqualTo("WAREHOUSE-SERVICE");
    }

    @Test
    void getMovementHistoryShouldReturnAscendingHistory() throws Exception {
        movementRepository.save(StockMovement.builder()
                .productId(101L)
                .warehouseId(5L)
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("25.0000"))
                .referenceId(9001L)
                .referenceType("PURCHASE_ORDER")
                .unitCost(new BigDecimal("149.5000"))
                .performedBy(12L)
                .notes("Initial receipt")
                .movementDate(LocalDateTime.of(2026, 4, 20, 10, 30))
                .balanceAfter(new BigDecimal("250.0000"))
                .createdBy("warehouse.staff@stockpro.com")
                .sourceService("WAREHOUSE-SERVICE")
                .build());
        movementRepository.save(StockMovement.builder()
                .productId(101L)
                .warehouseId(5L)
                .movementType(MovementType.STOCK_OUT)
                .quantity(new BigDecimal("5.0000"))
                .referenceId(9002L)
                .referenceType("SALES_ORDER")
                .unitCost(new BigDecimal("149.5000"))
                .performedBy(12L)
                .notes("Issued stock")
                .movementDate(LocalDateTime.of(2026, 4, 21, 10, 30))
                .balanceAfter(new BigDecimal("245.0000"))
                .createdBy("warehouse.staff@stockpro.com")
                .sourceService("WAREHOUSE-SERVICE")
                .build());

        mockMvc.perform(get("/api/v1/movements/history")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(1L, "manager@stockpro.com", "ADMIN"))
                        .param("productId", "101")
                        .param("warehouseId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("STOCK_IN"))
                .andExpect(jsonPath("$[1].movementType").value("STOCK_OUT"));
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

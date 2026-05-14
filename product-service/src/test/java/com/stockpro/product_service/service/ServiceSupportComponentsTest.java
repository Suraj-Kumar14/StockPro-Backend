package com.stockpro.product_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.stockpro.product_service.dto.WarehouseStockSnapshotDTO;
import com.stockpro.product_service.dto.request.CreateProductRequest;
import com.stockpro.product_service.dto.request.UpdateProductRequest;
import com.stockpro.product_service.entity.Product;
import com.stockpro.product_service.exception.InvalidProductDataException;
import com.stockpro.product_service.security.AuthenticatedUser;

class ServiceSupportComponentsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void productValidationService_shouldSanitizeCreateAndUpdateRequests() {
        ProductValidationService validationService = new ProductValidationService();

        CreateProductRequest createRequest = new CreateProductRequest();
        createRequest.setSku(" SKU-001 ");
        createRequest.setName("  Laptop  ");
        createRequest.setDescription("  Warehouse laptop  ");
        createRequest.setCategory(" Electronics ");
        createRequest.setBrand(" Dell ");
        createRequest.setUnitOfMeasure(" Piece ");
        createRequest.setBarcode(" BAR-001 ");

        UpdateProductRequest updateRequest = new UpdateProductRequest();
        updateRequest.setName("  Laptop Pro  ");
        updateRequest.setDescription("  Updated  ");
        updateRequest.setCategory(" Electronics ");
        updateRequest.setBrand(" Dell ");
        updateRequest.setUnitOfMeasure(" Piece ");
        updateRequest.setBarcode(" BAR-002 ");
        updateRequest.setIsActive(true);

        CreateProductRequest sanitizedCreate = validationService.sanitize(createRequest);
        UpdateProductRequest sanitizedUpdate = validationService.sanitize(updateRequest);

        assertEquals("SKU-001", sanitizedCreate.getSku());
        assertEquals("Laptop", sanitizedCreate.getName());
        assertEquals("Warehouse laptop", sanitizedCreate.getDescription());
        assertEquals("Electronics", sanitizedCreate.getCategory());
        assertEquals("Dell", sanitizedCreate.getBrand());
        assertEquals("Piece", sanitizedCreate.getUnitOfMeasure());
        assertEquals("BAR-001", sanitizedCreate.getBarcode());

        assertEquals("Laptop Pro", sanitizedUpdate.getName());
        assertEquals("Updated", sanitizedUpdate.getDescription());
        assertEquals("Electronics", sanitizedUpdate.getCategory());
        assertEquals("Dell", sanitizedUpdate.getBrand());
        assertEquals("Piece", sanitizedUpdate.getUnitOfMeasure());
        assertEquals("BAR-002", sanitizedUpdate.getBarcode());
        assertTrue(sanitizedUpdate.getIsActive());
    }

    @Test
    void productValidationService_shouldAcceptSupportedSkuFormatsAndRejectInvalidOnes() {
        ProductValidationService validationService = new ProductValidationService();

        assertEquals("SKU-001", validationService.normalizeSku(" SKU-001 "));
        assertEquals("CAN-INK-001", validationService.normalizeSku(" CAN-INK-001 "));

        InvalidProductDataException lowercaseException = assertThrows(
                InvalidProductDataException.class,
                () -> validationService.normalizeSku("sku-001"));
        assertEquals("SKU must be valid format like SKU-001 or CAN-INK-001", lowercaseException.getMessage());

        assertThrows(InvalidProductDataException.class, () -> validationService.normalizeSku("SKU001"));
        assertThrows(InvalidProductDataException.class, () -> validationService.normalizeSku("SK-001"));
    }

    @Test
    void productValidationService_shouldValidateBusinessRules() {
        ProductValidationService validationService = new ProductValidationService();

        assertDoesNotThrow(() -> validationService.validateBusinessRules(
                BigDecimal.ONE,
                BigDecimal.TEN,
                2,
                10,
                3));

        assertThrows(InvalidProductDataException.class, () -> validationService.validateBusinessRules(
                BigDecimal.ONE,
                BigDecimal.TEN,
                10,
                5,
                3));
    }

    @Test
    void productMapper_shouldMapEntityToResponse() {
        ProductMapper mapper = new ProductMapper();
        LocalDateTime now = LocalDateTime.now();
        Product product = Product.builder()
                .productId(1L)
                .sku("SKU-001")
                .name("Laptop")
                .description("Warehouse laptop")
                .category("Electronics")
                .brand("Dell")
                .unitOfMeasure("Piece")
                .costPrice(BigDecimal.valueOf(750))
                .sellingPrice(BigDecimal.valueOf(999))
                .reorderLevel(5)
                .maxStockLevel(25)
                .leadTimeDays(7)
                .imageUrl("https://example.com/laptop.png")
                .barcode("BAR-001")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        var response = mapper.toResponse(product);

        assertEquals(1L, response.getProductId());
        assertEquals("SKU-001", response.getSku());
        assertEquals("Laptop", response.getName());
        assertEquals("Electronics", response.getCategory());
        assertEquals("BAR-001", response.getBarcode());
        assertTrue(response.getIsActive());
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    void currentUserContext_shouldReturnUserIdFromPrincipalOrProfile() {
        AuthProfileGateway authProfileGateway = new AuthProfileGateway(RestClient.builder());
        CurrentUserContext currentUserContext = new CurrentUserContext(authProfileGateway);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(55L, "admin@stockpro.com", "ADMIN", "token-1"),
                        "token-1"));
        assertEquals(55L, currentUserContext.getActorId());

        AuthProfileGateway resolvingGateway = new AuthProfileGateway(RestClient.builder()) {
            @Override
            public Optional<Long> resolveUserId(String token) {
                return Optional.of(88L);
            }
        };
        CurrentUserContext resolvingContext = new CurrentUserContext(resolvingGateway);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(null, "manager@stockpro.com", "MANAGER", "token-2"),
                        "token-2"));

        assertEquals(88L, resolvingContext.getActorId());
        assertEquals("manager@stockpro.com", resolvingContext.getCurrentUser().email());
    }

    @Test
    void currentUserContext_shouldThrowWhenAuthenticationMissing() {
        CurrentUserContext currentUserContext = new CurrentUserContext(new AuthProfileGateway(RestClient.builder()));

        assertThrows(InvalidProductDataException.class, currentUserContext::getCurrentUser);
    }

    @Test
    void authProfileGateway_shouldResolveUserIdAndHandleErrors() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        AuthProfileGateway authProfileGateway = new AuthProfileGateway(restClientBuilder);
        ReflectionTestUtils.setField(authProfileGateway, "authServiceBaseUrl", "http://auth-service");

        mockServer.expect(requestTo("http://auth-service/auth/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"userId\":42,\"name\":\"Admin\",\"email\":\"admin@stockpro.com\",\"role\":\"ADMIN\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(42L), authProfileGateway.resolveUserId("token-1"));

        mockServer.reset();
        mockServer.expect(requestTo("http://auth-service/auth/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertEquals(Optional.empty(), authProfileGateway.resolveUserId("token-2"));
    }

    @Test
    void warehouseInventoryGateway_shouldAggregateAvailabilityAndUsage() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        WarehouseInventoryGateway warehouseInventoryGateway = new WarehouseInventoryGateway(restClientBuilder);
        ReflectionTestUtils.setField(warehouseInventoryGateway, "warehouseServiceBaseUrl", "http://warehouse-service/stock");

        mockServer.expect(requestTo("http://warehouse-service/stock/product/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                [
                                  {"stockId":1,"warehouseId":10,"productId":1,"quantity":8,"reservedQuantity":2,"availableQuantity":6},
                                  {"stockId":2,"warehouseId":11,"productId":1,"quantity":4,"reservedQuantity":0,"availableQuantity":4}
                                ]
                                """,
                        MediaType.APPLICATION_JSON));

        assertEquals(Optional.of(10), warehouseInventoryGateway.getAvailableQuantity(1L));

        mockServer.reset();
        mockServer.expect(requestTo("http://warehouse-service/stock/product/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                                [
                                  {"stockId":1,"warehouseId":10,"productId":1,"quantity":0,"reservedQuantity":0,"availableQuantity":0},
                                  {"stockId":2,"warehouseId":11,"productId":1,"quantity":3,"reservedQuantity":1,"availableQuantity":2}
                                ]
                                """,
                        MediaType.APPLICATION_JSON));

        assertTrue(warehouseInventoryGateway.hasInventoryUsage(1L));
    }

    @Test
    void warehouseInventoryGateway_shouldHandleRemoteFailures() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        WarehouseInventoryGateway warehouseInventoryGateway = new WarehouseInventoryGateway(restClientBuilder);
        ReflectionTestUtils.setField(warehouseInventoryGateway, "warehouseServiceBaseUrl", "http://warehouse-service/stock");

        mockServer.expect(requestTo("http://warehouse-service/stock/product/2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertEquals(Optional.empty(), warehouseInventoryGateway.getAvailableQuantity(2L));

        mockServer.reset();
        mockServer.expect(requestTo("http://warehouse-service/stock/product/2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertFalse(warehouseInventoryGateway.hasInventoryUsage(2L));
    }

    @Test
    void noOpServicesAndRecords_shouldBehavePredictably() {
        ProductAuditEntry auditEntry = ProductAuditEntry.builder()
                .actorId(1L)
                .action("PRODUCT_CREATED")
                .entityType("PRODUCT")
                .entityId(10L)
                .oldValue(null)
                .newValue("new")
                .timestamp(LocalDateTime.now())
                .serviceName("product-service")
                .build();
        ProductLifecycleEvent lifecycleEvent = ProductLifecycleEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("PRODUCT_CREATED")
                .productId(10L)
                .sku("SKU-001")
                .name("Laptop")
                .category("Electronics")
                .brand("Dell")
                .barcode("BAR-001")
                .reorderLevel(5)
                .maxStockLevel(25)
                .leadTimeDays(7)
                .isActive(true)
                .actorId(1L)
                .eventTime(LocalDateTime.now())
                .oldValue(null)
                .newValue("new")
                .build();
        WarehouseStockSnapshotDTO snapshot = new WarehouseStockSnapshotDTO();
        snapshot.setAvailableQuantity(4);

        assertEquals(1L, auditEntry.actorId());
        assertEquals("SKU-001", lifecycleEvent.sku());
        assertEquals(4, snapshot.getAvailableQuantity());

        assertDoesNotThrow(() -> new NoOpProductAuditService().record(auditEntry));
        assertDoesNotThrow(() -> {
            NoOpProductEventPublisher publisher = new NoOpProductEventPublisher();
            publisher.publishProductCreated(lifecycleEvent);
            publisher.publishProductUpdated(lifecycleEvent);
            publisher.publishProductActivated(lifecycleEvent);
            publisher.publishProductDeactivated(lifecycleEvent);
            publisher.publishProductReorderConfigChanged(lifecycleEvent);
            publisher.publishProductDeleted(lifecycleEvent);
        });
        assertFalse(new NoOpPurchaseOrderUsageGateway().hasPurchaseOrderUsage(10L));
    }
}

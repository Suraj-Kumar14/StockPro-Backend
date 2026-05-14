package com.stockpro.purchaseservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import com.stockpro.purchaseservice.exception.WarehouseStockUpdateException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpWarehouseGatewayTest {

    private RestClient.Builder restClientBuilder;
    private HttpWarehouseGateway gateway;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        gateway = new HttpWarehouseGateway(restClientBuilder, new DownstreamAuthSupport());
        ReflectionTestUtils.setField(gateway, "warehouseServiceBaseUrl", "http://WAREHOUSE-SERVICE/api/v1/warehouses");
    }

    @Test
    void receivePurchaseOrderCallsWarehouseServiceEndpoint() {
        server.expect(requestTo("http://WAREHOUSE-SERVICE/api/v1/stocks/receive"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        gateway.increaseStock(2L, 100L, 4, 10L, "PO-10", BigDecimal.TEN, "Received", null);

        server.verify();
    }

    @Test
    void warehouseServerFailureMapsToGatewayException() {
        server.expect(requestTo("http://WAREHOUSE-SERVICE/api/v1/stocks/receive"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        WarehouseStockUpdateException exception = assertThrows(
                WarehouseStockUpdateException.class,
                () -> gateway.increaseStock(2L, 100L, 4, 10L, "PO-10", BigDecimal.TEN, "Received", null));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }

    @Test
    void resolveServiceRootUrlKeepsWarehouseServiceNameInsteadOfLocalhost() {
        ReflectionTestUtils.setField(gateway, "warehouseServiceBaseUrl", "WAREHOUSE-SERVICE/api/v1/warehouses/");

        String resolved = (String) ReflectionTestUtils.invokeMethod(gateway, "resolveServiceRootUrl");

        assertEquals("http://WAREHOUSE-SERVICE", resolved);
        assertFalse(resolved.contains("localhost"));
    }
}

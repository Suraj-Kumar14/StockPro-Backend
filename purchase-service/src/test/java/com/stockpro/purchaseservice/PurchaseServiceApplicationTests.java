package com.stockpro.purchaseservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "JWT_SECRET=test-secret-key-for-purchase-service-2026",
        "PRODUCT_SERVICE_BASE_URL=http://localhost:8081/api/v1/products",
        "SUPPLIER_SERVICE_BASE_URL=http://localhost:8082/api/v1/suppliers",
        "WAREHOUSE_SERVICE_BASE_URL=http://localhost:8083/api/v1/warehouses",
        "RABBITMQ_USERNAME=guest",
        "RABBITMQ_PASSWORD=guest"
})
class PurchaseServiceApplicationTests {
    @Test
    void contextLoads() { }
}

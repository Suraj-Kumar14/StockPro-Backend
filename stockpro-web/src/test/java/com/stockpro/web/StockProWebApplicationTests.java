package com.stockpro.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "api.gateway.base-url=http://localhost:8080"
})
class StockProWebApplicationTests {

    @Test
    void contextLoads() {
    }
}

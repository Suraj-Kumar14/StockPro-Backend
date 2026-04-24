package com.stockpro.product;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.jwt.secret=ThisIsASecretKeyForJwtTokenGenerationThatMustBeAtLeast32BytesLong12345",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class ProductServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}

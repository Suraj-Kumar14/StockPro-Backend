package com.stockpro.purchaseservice.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {

    private final RestClientConfig config = new RestClientConfig();

    @Test
    void createsPrimaryRestClientBuilder() throws Exception {
        RestClient.Builder defaultBuilder = config.restClientBuilder();

        assertNotNull(defaultBuilder);

        Method method = RestClientConfig.class.getDeclaredMethod("restClientBuilder");
        assertTrue(method.isAnnotationPresent(Bean.class));
        assertTrue(method.isAnnotationPresent(Primary.class));
    }
}

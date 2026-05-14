package com.stockpro.purchaseservice.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {

    private final RestClientConfig config = new RestClientConfig();

    @Test
    void createsPlainAndLoadBalancedBuilders() throws Exception {
        RestClient.Builder defaultBuilder = config.restClientBuilder();
        RestClient.Builder loadBalancedBuilder = config.loadBalancedRestClientBuilder();

        assertNotNull(defaultBuilder);
        assertNotNull(loadBalancedBuilder);

        Method method = RestClientConfig.class.getDeclaredMethod("loadBalancedRestClientBuilder");
        assertTrue(method.isAnnotationPresent(LoadBalanced.class));
        assertTrue(method.isAnnotationPresent(Qualifier.class));
    }
}

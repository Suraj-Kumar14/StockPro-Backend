package com.stockpro.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"eureka.client.enabled=false",
				"spring.cloud.discovery.enabled=false",
				"eureka.client.fetch-registry=false",
				"eureka.client.register-with-eureka=false",
				"jwt.secret=stockpro-local-jwt-secret-2026-change-this-before-production-8f4b2c9d7e1a6f3c"
		}
)
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void contextLoads() {
	}

	@Test
	void actuatorHealthIsPublic() {
		webTestClient.get()
				.uri("/actuator/health")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("UP");
	}

	@Test
	void actuatorInfoIsPublic() {
		webTestClient.get()
				.uri("/actuator/info")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.app.name").isEqualTo("API-GATEWAY");
	}

	@Test
	void actuatorGatewayRoutesIsPublic() {
		webTestClient.get()
				.uri("/actuator/gateway/routes")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$").isArray();
	}

	@Test
	void protectedRouteRequiresJwt() {
		webTestClient.get()
				.uri("/api/v1/products")
				.exchange()
				.expectStatus().isUnauthorized()
				.expectBody()
				.jsonPath("$.error").isEqualTo("Missing Authorization header");
	}
}

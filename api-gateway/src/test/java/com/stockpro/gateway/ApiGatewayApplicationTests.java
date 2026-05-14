package com.stockpro.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "JWT_SECRET=12345678901234567890123456789012")
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}

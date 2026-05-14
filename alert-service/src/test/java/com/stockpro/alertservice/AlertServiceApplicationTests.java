package com.stockpro.alertservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AlertServiceApplicationTests {

	@MockBean
	private JavaMailSender javaMailSender;

	@Test
	void contextLoads() {
	}

}

package com.bank.account;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class AccountServiceApplicationTests {

	@MockitoBean
	private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

	@Test
	void contextLoads() {
	}
	@Test
	void main_shouldStartApplication() {

		try (MockedStatic<SpringApplication> mocked =
					 mockStatic(SpringApplication.class)) {


			AccountServiceApplication.main(new String[]{});


			mocked.verify(() ->
					SpringApplication.run(
							AccountServiceApplication.class,
							new String[]{}
					)
			);
		}
	}

}

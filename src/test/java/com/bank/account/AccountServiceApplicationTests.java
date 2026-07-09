package com.bank.account;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class AccountServiceApplicationTests {

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

package com.bank.account.client;

import com.bank.account.client.dto.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerClientTest {

    @Mock
    private WebClient.Builder builder;

    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private CustomerClient customerClient;

    @BeforeEach
    void setUp() {
        when(builder.build()).thenReturn(webClient);
        customerClient = new CustomerClient(builder);
    }

    @Test
    void customerExists_shouldReturnTrue() {

        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec)
                .when(requestHeadersUriSpec)
                .uri(
                        eq("http://customer-service/customers/{id}"),
                        eq("1")
                );
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenReturn(Mono.just(ResponseEntity.ok().build()));

        StepVerifier.create(customerClient.customerExists("1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void getCustomerById_shouldReturnCustomer() {

        Customer customer = Customer.builder()
                .id("1")
                .build();
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec)
                .when(requestHeadersUriSpec)
                .uri(
                        eq("http://customer-service/customers/{id}"),
                        eq("1")
                );
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Customer.class))
                .thenReturn(Mono.just(customer));

        StepVerifier.create(customerClient.getCustomerById("1"))
                .expectNext(customer)
                .verifyComplete();
    }

    @Test
    void customerExistsFallback_shouldReturnFalse() {

        StepVerifier.create(
                        customerClient.customerExistsFallback(
                                "1",
                                new RuntimeException("error")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void getCustomerFallback_shouldReturnError() {

        StepVerifier.create(
                        customerClient.getCustomerFallback(
                                "1",
                                new RuntimeException("error")))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                ex.getMessage().equals("Customer Service is unavailable"))
                .verify();
    }
}

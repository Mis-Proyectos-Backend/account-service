package com.bank.account.client;

import com.bank.account.client.dto.Credit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CreditClientTest {


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


    private CreditClient creditClient;



    @BeforeEach
    void setUp() {

        when(builder.build())
                .thenReturn(webClient);

        creditClient = new CreditClient(builder);
    }



    @Test
    void getCreditsByCustomer_shouldReturnCredits() {


        Credit credit = Credit.builder()
                .id("cr1")
                .build();


        doReturn(requestHeadersUriSpec)
                .when(webClient)
                .get();


        doReturn(requestHeadersSpec)
                .when(requestHeadersUriSpec)
                .uri(
                        eq("http://credit-service/credits/customer/{id}"),
                        eq("1")
                );


        when(requestHeadersSpec.retrieve())
                .thenReturn(responseSpec);


        when(responseSpec.bodyToFlux(Credit.class))
                .thenReturn(Flux.just(credit));



        StepVerifier.create(
                        creditClient.getCreditsByCustomer("1")
                )
                .expectNext(credit)
                .verifyComplete();

    }





    @Test
    void fallbackCredits_shouldReturnEmptyFlux() {


        StepVerifier.create(
                        creditClient.fallbackCredits(
                                "1",
                                new RuntimeException("error")
                        )
                )
                .verifyComplete();

    }

    @Test
    void hasOverdueDebt_shouldReturnTrue() {

        when(webClient.get())
                .thenReturn(requestHeadersUriSpec);

        when(requestHeadersUriSpec.uri(
                "http://credit-service/credits/customers/{customerId}/overdue",
                "c1"))
                .thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.retrieve())
                .thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Boolean.class))
                .thenReturn(Mono.just(true));

        StepVerifier.create(creditClient.hasOverdueDebt("c1"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void fallbackHasOverdueDebt_shouldReturnFalse() {

        StepVerifier.create(
                        creditClient.fallbackHasOverdueDebt(
                                "c1",
                                new RuntimeException("Service unavailable")
                        )
                )
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void hasOverdueDebt_shouldReturnFalse() {

        when(webClient.get())
                .thenReturn(requestHeadersUriSpec);

        when(requestHeadersUriSpec.uri(
                "http://credit-service/credits/customers/{customerId}/overdue",
                "c1"))
                .thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.retrieve())
                .thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Boolean.class))
                .thenReturn(Mono.just(false));

        StepVerifier.create(creditClient.hasOverdueDebt("c1"))
                .expectNext(false)
                .verifyComplete();
    }



}
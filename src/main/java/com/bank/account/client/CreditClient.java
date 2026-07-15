package com.bank.account.client;

import com.bank.account.client.dto.Credit;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CreditClient {

    private final WebClient webClient;

    public CreditClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @CircuitBreaker(name = "creditService", fallbackMethod = "fallbackCredits")
    public Flux<Credit> getCreditsByCustomer(String customerId) {

        return webClient.get()
                .uri("http://credit-service/credits/customer/{id}", customerId)
                .retrieve()
                .bodyToFlux(Credit.class);
    }

    public Flux<Credit> fallbackCredits(String customerId, Throwable ex) {
        return Flux.empty();
    }

    @CircuitBreaker(name = "creditService", fallbackMethod = "fallbackHasOverdueDebt")
    public Mono<Boolean> hasOverdueDebt(String customerId) {

        return webClient.get()
                .uri("http://credit-service/credits/customers/{customerId}/overdue", customerId)
                .retrieve()
                .bodyToMono(Boolean.class);
    }
    public Mono<Boolean> fallbackHasOverdueDebt(String customerId, Throwable ex) {
        return Mono.just(false);
    }
}

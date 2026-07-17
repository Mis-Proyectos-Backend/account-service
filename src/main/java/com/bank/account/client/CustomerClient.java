package com.bank.account.client;

import com.bank.account.client.dto.Customer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CustomerClient {

    private final WebClient webClient;

    public CustomerClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "customerExistsFallback")
    public Mono<Boolean> customerExists(String customerId) {
        return webClient.get()
                .uri("http://customer-service/customers/{id}", customerId)
                .retrieve()
                .toBodilessEntity()
                .map(response -> true);
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "getCustomerFallback")
    public Mono<Customer> getCustomerById(String customerId) {
        return webClient.get()
                .uri("http://customer-service/customers/{id}", customerId)
                .retrieve()
                .bodyToMono(Customer.class);
    }
    public Mono<Boolean> customerExistsFallback(String customerId, Throwable ex) {
        return Mono.just(false);
    }

    public Mono<Customer> getCustomerFallback(String customerId, Throwable ex) {
        return Mono.error(
                new RuntimeException("Customer Service is unavailable", ex));
    }
}

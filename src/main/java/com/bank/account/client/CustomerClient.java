package com.bank.account.client;

import com.bank.account.client.dto.Customer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CustomerClient {

    private final WebClient webClient;

    public CustomerClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Boolean> customerExists(String customerId) {
        return webClient.get()
                .uri("http://localhost:8081/customers/" + customerId)
                .retrieve()
                .toBodilessEntity()
                .map(response -> true)
                .onErrorReturn(false);
    }

    public Mono<Customer> getCustomerById(String customerId) {
        return webClient.get()
                .uri("http://localhost:8081/customers/" + customerId)
                .retrieve()
                .bodyToMono(Customer.class);
    }
}

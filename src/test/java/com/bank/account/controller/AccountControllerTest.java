package com.bank.account.controller;

import com.bank.account.client.dto.WithdrawRequest;
import com.bank.account.dto.TransferRequest;
import com.bank.account.enums.PaymentMethod;
import com.bank.account.model.Account;
import com.bank.account.enums.AccountType;
import com.bank.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class AccountControllerTest {


    private AccountService service;
    private WebTestClient webTestClient;


    @BeforeEach
    void setUp() {

        service = Mockito.mock(AccountService.class);

        AccountController controller =
                new AccountController(service);

        webTestClient = WebTestClient
                .bindToController(controller)
                .build();
    }



    @Test
    void create_shouldReturnAccount() {
        Account account = Account.builder()
                .id("a1")
                .customerId("c1")
                .type(AccountType.SAVINGS)
                .balance(BigDecimal.ZERO)
                .build();

        when(service.create(any(Account.class)))
                .thenReturn(Mono.just(account));
        webTestClient.post()
                .uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(account)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo("a1")
                .jsonPath("$.customerId")
                .isEqualTo("c1");
    }

    @Test
    void getById_shouldReturnAccount() {
        Account account = Account.builder()
                .id("a1")
                .customerId("c1")
                .type(AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(100))
                .build();
        when(service.getById("a1"))
                .thenReturn(Mono.just(account));

        webTestClient.get()
                .uri("/accounts/a1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo("a1");
    }

    @Test
    void getAll_shouldReturnAccounts() {
        Account account1 = Account.builder()
                .id("a1")
                .build();
        Account account2 = Account.builder()
                .id("a2")
                .build();
        when(service.getAll())
                .thenReturn(
                        Flux.just(account1, account2)
                );
        webTestClient.get()
                .uri("/accounts")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Account.class)
                .hasSize(2);
    }

    @Test
    void update_shouldReturnUpdatedAccount() {
        Account account = Account.builder()
                .id("a1")
                .customerId("c1")
                .build();
        when(service.update(
                eq("a1"),
                any(Account.class)
        ))
                .thenReturn(Mono.just(account));
        webTestClient.put()
                .uri("/accounts/a1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(account)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo("a1");
    }




    @Test
    void deposit_shouldReturnAccount() {
        Account account = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(200))
                .build();
        when(service.deposit(
                eq("a1"),
                eq(BigDecimal.valueOf(100))
        ))
                .thenReturn(Mono.just(account));
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/accounts/a1/deposit")
                        .queryParam("amount", 100)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.balance")
                .isEqualTo(200);
    }

    @Test
    void withdraw_shouldReturnAccount() {

        Account account = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(50))
                .build();


        WithdrawRequest request = WithdrawRequest.builder()
                .amount(BigDecimal.valueOf(50))
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .build();


        when(service.withdraw(
                eq("a1"),
                any(WithdrawRequest.class)
        ))
                .thenReturn(Mono.just(account));


        webTestClient.post()
                .uri("/accounts/a1/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo("a1");
    }





    @Test
    void delete_shouldCompleteSuccessfully() {
        when(service.delete("a1"))
                .thenReturn(Mono.empty());
        webTestClient.delete()
                .uri("/accounts/a1")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void transfer_shouldReturnOk() {
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId("a1");
        request.setDestinationAccountId("a2");
        request.setAmount(BigDecimal.valueOf(200));

        when(service.transfer(
                "a1",
                "a2",
                BigDecimal.valueOf(200)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/accounts/transfer")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        verify(service).transfer(
                "a1",
                "a2",
                BigDecimal.valueOf(200));
    }

    @Test
    void transfer_whenServiceFails_shouldReturn5xx() {

        TransferRequest request = new TransferRequest();
        request.setSourceAccountId("a1");
        request.setDestinationAccountId("a2");
        request.setAmount(BigDecimal.valueOf(200));

        when(service.transfer(any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("Insufficient funds")));

        webTestClient.post()
                .uri("/accounts/transfer")
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getByCustomerId_shouldReturnAccount() {
        Flux<Account> accounts = Flux.just(Account.builder()
                .id("a1")
                .customerId("c1")
                .type(AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(100))
                .build());

        when(service.getByCustomerId("c1"))
                .thenReturn(accounts);

        webTestClient.get()
                .uri("/accounts/customer/c1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].id")
                .isEqualTo("a1");
    }

}
package com.bank.account.service;

import com.bank.account.client.dto.WithdrawRequest;
import com.bank.account.dto.TransferRequest;
import com.bank.account.enums.PaymentMethod;
import com.bank.account.model.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AccountService {

    Mono<Account> create(Account account);

    Mono<Account> getById(String id);

    Flux<Account> getAll();

    Mono<Account> update(String id, Account account);

    Mono<Account> deposit(String id, BigDecimal amount);

    Mono<Account> withdraw(String id, WithdrawRequest request);

    Mono<Void> delete(String id);

    Mono<Void> transfer(TransferRequest request);

    Flux<Account> getByCustomerId(String customerId);
}

package com.bank.account.repository;

import com.bank.account.enums.AccountType;
import com.bank.account.model.Account;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountRepository extends ReactiveMongoRepository<Account, String> {

    Flux<Account> findByCustomerId(String customerId);

    Mono<Account> findByAccountNumber(String accountNumber);

    Mono<Boolean> existsByCustomerIdAndType(
            String customerId,
            AccountType type);
}

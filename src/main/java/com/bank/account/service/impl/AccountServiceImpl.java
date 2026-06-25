package com.bank.account.service.impl;

import com.bank.account.client.CustomerClient;
import com.bank.account.client.dto.Customer;
import com.bank.account.enums.AccountType;
import com.bank.account.model.Account;
import com.bank.account.repository.AccountRepository;
import com.bank.account.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repository;
    private final CustomerClient customerClient;

    public AccountServiceImpl(AccountRepository repository, CustomerClient customerClient) {
        this.repository = repository;
        this.customerClient = customerClient;
    }

    @Override
    public Mono<Account> create(Account account) {
        return customerClient.getCustomerById(account.getCustomerId())
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Customer not found")))
                .flatMap(customer -> validateAndSave(customer, account));
    }

    private Mono<Account> validateAndSave(Customer customer, Account account) {

        if (!customer.getCustomerType().equals("PERSONAL")
                && !customer.getCustomerType().equals("BUSINESS")) {
            return Mono.error(new RuntimeException("Invalid customer type"));
        }
        boolean isBusiness = "BUSINESS".equals(customer.getCustomerType());

        if (!isBusiness) {
            return repository
                    .existsByCustomerIdAndType(
                            account.getCustomerId(),
                            account.getType())
                    .flatMap(exists -> {

                        if (exists) {
                            return Mono.error(
                                    new RuntimeException(
                                            "Customer already has this account type"));
                        }
                        return saveAccount(account);
                    });
        } else {
            boolean invalidAccountType = account.getType() == AccountType.SAVINGS || account.getType() == AccountType.FIXED_TERM;
            if (invalidAccountType) {
                return Mono.error(new RuntimeException(
                        "Business customers can only have CHECKING accounts"));
            }
            return saveAccount(account);
        }
    }

    private Mono<Account> saveAccount(Account account) {
        account.setCreatedAt(LocalDate.now());
        account.setBalance(BigDecimal.ZERO);
        return repository.save(account);
    }

    @Override
    public Mono<Account> getById(String id) {
        return repository.findById(id);
    }

    @Override
    public Flux<Account> getAll() {
        return repository.findAll();
    }

    @Override
    public Mono<Account> update(String id, Account account) {

        return repository.findById(id)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Account not found")))
                .flatMap(existingAccount -> {

                    existingAccount.setType(account.getType());

                    return repository.save(existingAccount);
                });
    }


    @Override
    public Mono<Account> deposit(String id, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be greater than zero"));
        }
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Account not found")))
                .flatMap(account -> {
                    account.setBalance(account.getBalance().add(amount));
                    return repository.save(account);
                });
    }

    @Override
    public Mono<Account> withdraw(String id, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be greater than zero"));
        }
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Account not found")))
                .flatMap(account -> {

                    if (account.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new RuntimeException("Insufficient funds"));
                    }

                    account.setBalance(account.getBalance().subtract(amount));
                    return repository.save(account);
                });
    }

    @Override
    public Mono<Void> delete(String id) {

        return repository.findById(id)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Account not found")))
                .flatMap(account -> repository.delete(account));
    }
}

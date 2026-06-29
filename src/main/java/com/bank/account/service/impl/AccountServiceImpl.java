package com.bank.account.service.impl;

import com.bank.account.client.CustomerClient;
import com.bank.account.client.dto.Customer;
import com.bank.account.enums.AccountType;
import com.bank.account.enums.MovementType;
import com.bank.account.model.Account;
import com.bank.account.model.Movement;
import com.bank.account.repository.AccountRepository;
import com.bank.account.repository.MovementRepository;
import com.bank.account.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repository;
    private final CustomerClient customerClient;
    private final MovementRepository movementRepository;

    public AccountServiceImpl(AccountRepository repository,
                              CustomerClient customerClient,
                              MovementRepository movementRepository) {
        this.repository = repository;
        this.customerClient = customerClient;
        this.movementRepository = movementRepository;
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
            if (account.getType() == AccountType.FIXED_TERM) {

                if (account.getMovementDay() == null
                        || account.getMovementDay() < 1
                        || account.getMovementDay() > 31) {

                    return Mono.error(
                            new RuntimeException("Movement day must be between 1 and 31"));
                }
                return saveAccount(account);
            }
            return repository.existsByCustomerIdAndType(
                            account.getCustomerId(),
                            account.getType())
                    .flatMap(exists -> {

                        if (exists) {
                            return Mono.error(new RuntimeException(
                                    "Customer already has this account type"));
                        }

                        return saveAccount(account);
                    });

        } else {

            boolean invalidAccountType =
                    account.getType() == AccountType.SAVINGS
                            || account.getType() == AccountType.FIXED_TERM;

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
                .flatMap(account ->
                        validateFixedTermAccount(account)
                                .then(Mono.just(account)))
                .flatMap(account -> {
                    account.setBalance(account.getBalance().add(amount));
                    return repository.save(account)
                            .flatMap(savedAccount ->
                                    saveMovement(
                                            savedAccount,
                                            MovementType.DEPOSIT,
                                            amount)
                                            .thenReturn(savedAccount));
                });
    }

    @Override
    public Mono<Account> withdraw(String id, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Amount must be greater than zero"));
        }
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Account not found")))
                .flatMap(account ->
                        validateFixedTermAccount(account)
                                .then(Mono.just(account)))
                .flatMap(account -> {
                    if (account.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new RuntimeException("Insufficient funds"));
                    }

                    account.setBalance(account.getBalance().subtract(amount));
                    return repository.save(account)
                            .flatMap(savedAccount ->
                                    saveMovement(
                                            savedAccount,
                                            MovementType.WITHDRAW,
                                            amount)
                                            .thenReturn(savedAccount));
                });
    }

    @Override
    public Mono<Void> delete(String id) {

        return repository.findById(id)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Account not found")))
                .flatMap(account -> repository.delete(account));
    }

    private Mono<Void> saveMovement(Account account,
                                    MovementType movementType,
                                    BigDecimal amount) {
        Movement movement = Movement.builder()
                .accountId(account.getId())
                .movementType(movementType)
                .amount(amount)
                .balanceAfterMovement(account.getBalance())
                .movementDate(LocalDateTime.now())
                .build();

        return movementRepository.save(movement).then();
    }

    private Mono<Void> validateFixedTermAccount(Account account) {
        if (account.getType() != AccountType.FIXED_TERM) {
            return Mono.empty();
        }
        int today = LocalDate.now().getDayOfMonth();

        if (!Integer.valueOf(today).equals(account.getMovementDay())) {
            return Mono.error(new RuntimeException(
                    "Operations are only allowed on day " + account.getMovementDay()));
        }
        return Mono.empty();
    }
}

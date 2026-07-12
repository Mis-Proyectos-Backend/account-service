package com.bank.account.service.impl;

import com.bank.account.client.CreditClient;
import com.bank.account.client.CustomerClient;
import com.bank.account.client.dto.Customer;
import com.bank.account.config.AccountProperties;
import com.bank.account.enums.AccountType;
import com.bank.account.enums.CreditType;
import com.bank.account.enums.CustomerProfile;
import com.bank.account.enums.MovementType;
import com.bank.account.model.Account;
import com.bank.account.repository.AccountRepository;
import com.bank.account.service.AccountService;
import com.bank.account.producer.AccountMovementProducer;
import com.bank.account.event.AccountMovementEvent;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository repository;
    private final CustomerClient customerClient;
    private final CreditClient creditClient;
    private final AccountMovementProducer accountMovementProducer;
    private final AccountProperties accountProperties;
    private final ReactiveRedisTemplate<String, Account> redisTemplate;


    public AccountServiceImpl(AccountRepository repository,
                              CustomerClient customerClient,
                              CreditClient creditClient,
                              AccountMovementProducer accountMovementProducer,
                              AccountProperties accountProperties,
                              ReactiveRedisTemplate<String, Account> redisTemplate) {
        this.repository = repository;
        this.customerClient = customerClient;
        this.creditClient = creditClient;
        this.accountMovementProducer = accountMovementProducer;
        this.accountProperties = accountProperties;
        this.redisTemplate = redisTemplate;
    }

    /* ---------------- Public API (ordered) ---------------- */

    @Override
    public Mono<Account> create(Account account) {
        return customerClient.getCustomerById(account.getCustomerId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found")))
                .flatMap(customer -> validateAndSave(customer, account));
    }

    @Override
    public Mono<Account> getById(String id) {

        String key = "account:" + id;

        return redisTemplate.opsForValue()
                .get(key)
                .switchIfEmpty(
                        repository.findById(id)
                                .switchIfEmpty(
                                        Mono.error(new RuntimeException("Cuenta no encontrada"))
                                )
                                .flatMap(account ->
                                        redisTemplate.opsForValue()
                                                .set(key, account)
                                                .thenReturn(account)
                                )
                );
    }

    @Override
    public Flux<Account> getAll() {
        return repository.findAll();
    }

    @Override
    public Flux<Account> getByCustomerId(String customerId) {
        return repository.findByCustomerId(customerId);
    }

    @Override
    public Mono<Account> update(String id, Account account) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Cuenta no encontrada")))
                .flatMap(existingAccount -> {

                    existingAccount.setType(account.getType());

                    return repository.save(existingAccount);
                })
                .flatMap(saved ->
                        redisTemplate.delete("account:" + saved.getId())
                                .thenReturn(saved)
                );
    }

    @Override
    public Mono<Account> deposit(String id, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("El importe debe ser mayor que cero."));
        }

        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Cuenta no encontrada")))
                .flatMap(account -> validateFixedTermAccount(account).then(Mono.just(account)))
                .flatMap(this::applyTransactionCommission)
                .flatMap(account -> {
                    account.setBalance(account.getBalance().add(amount));
                    account.setTransactionCount(account.getTransactionCount() + 1);
                    return repository.save(account)
                            .flatMap(savedAccount ->
                                            saveMovement(savedAccount, MovementType.DEPOSIT, amount)
                                                    .then(redisTemplate.delete("account:" + savedAccount.getId()))
                                                    .thenReturn(savedAccount)
                            );
                });
    }

    @Override
    public Mono<Account> withdraw(String id, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("El importe debe ser mayor que cero."));
        }

        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Cuenta no encontrada")))
                .flatMap(account -> validateFixedTermAccount(account).then(Mono.just(account)))
                .flatMap(this::applyTransactionCommission)
                .flatMap(account -> {
                    if (account.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new RuntimeException("Fondos insuficientes"));
                    }

                    account.setBalance(account.getBalance().subtract(amount));
                    account.setTransactionCount(account.getTransactionCount() + 1);
                    return repository.save(account)
                            .flatMap(savedAccount ->
                                            saveMovement(savedAccount, MovementType.WITHDRAW, amount)
                                                    .then(redisTemplate.delete("account:" + savedAccount.getId()))
                                                    .thenReturn(savedAccount)
                            );
                });
    }

    @Override
    public Mono<Void> transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("El importe debe ser mayor que cero."));
        }

        Mono<Account> fromAccountMono = repository.findById(fromAccountId)
                .switchIfEmpty(Mono.error(new RuntimeException("No se encontró la cuenta de origen.")));

        Mono<Account> toAccountMono = repository.findById(toAccountId)
                .switchIfEmpty(Mono.error(new RuntimeException("Cuenta de destino no encontrada")));

        return Mono.zip(fromAccountMono, toAccountMono)
                .flatMap(accounts -> validateTransfer(accounts, amount))
                .flatMap(accounts -> executeTransfer(accounts, amount));
    }

    @Override
    public Mono<Void> delete(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Cuenta no encontrada")))
                .flatMap(account ->
                        repository.delete(account)
                                .then(redisTemplate.delete("account:" + id))
                                .then()
                );
    }
    /* ---------------- Private helpers (grouped) ---------------- */

    // Save / validation helpers
    private Mono<Account> validateAndSave(Customer customer, Account account) {
        if (customer.getCustomerType() != com.bank.account.enums.CustomerType.PERSONAL && customer.getCustomerType() != com.bank.account.enums.CustomerType.BUSINESS) {
            return Mono.error(new RuntimeException("Tipo de cliente no válido"));
        }
        boolean isBusiness = customer.getCustomerType() == com.bank.account.enums.CustomerType.BUSINESS;

        if (!isBusiness) {
            if (account.getType() == AccountType.FIXED_TERM) {
                if (account.getMovementDay() == null || account.getMovementDay() < 1 || account.getMovementDay() > 31) {
                    return Mono.error(new RuntimeException("El día del movimiento debe estar entre 1 y 31."));
                }
                return validateInitialBalance(account)
                        .then(validateCustomerProfile(customer, account))
                        .then(saveAccount(account));
            }

            return repository.existsByCustomerIdAndType(account.getCustomerId(), account.getType())
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(new RuntimeException("El cliente ya tiene este tipo de cuenta"));
                        }
                        return validateInitialBalance(account)
                                .then(validateCustomerProfile(customer, account))
                                .then(saveAccount(account));
                    });
        } else {
            boolean invalidAccountType = account.getType() == AccountType.SAVINGS || account.getType() == AccountType.FIXED_TERM;
            if (invalidAccountType) {
                return Mono.error(new RuntimeException("Los clientes empresariales solo pueden tener cuentas corrientes."));
            }
            return validateInitialBalance(account)
                    .then(validateCustomerProfile(customer, account))
                    .then(saveAccount(account));
        }

    }

    private Mono<Void> validateInitialBalance(Account account) {
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            return Mono.error(new RuntimeException("Initial balance cannot be negative"));
        }
        return Mono.empty();
    }

    private Mono<Account> saveAccount(Account account) {

        initializeAccountConfiguration(account);
        account.setCreatedAt(LocalDate.now());
        account.setTransactionCount(0);

        return repository.save(account)
                .flatMap(saved ->
                        redisTemplate.opsForValue()
                                .set("account:" + saved.getId(), saved)
                                .thenReturn(saved)
                );
    }

    private void initializeAccountConfiguration(Account account) {

        AccountProperties.AccountConfig config =
                accountProperties.getByType(account.getType());

        account.setFreeTransactions(config.getFreeTransactions());
        account.setTransactionCommission(config.getTransactionCommission());
    }

    // Customer profile checks
    private Mono<Void> validateCustomerProfile(Customer customer, Account account) {
        if (customer.getCustomerProfile() == CustomerProfile.VIP) {
            if (account.getType() != AccountType.SAVINGS) {
                return Mono.error(new RuntimeException("Los clientes VIP solo pueden abrir cuentas de ahorro."));
            }
            return hasCreditCard(customer.getId())
                    .flatMap(hasCard -> hasCard ? Mono.empty() : Mono.error(new RuntimeException("El cliente VIP debe tener una tarjeta de crédito.")));
        }

        if (customer.getCustomerProfile() == CustomerProfile.PYME) {
            if (account.getType() != AccountType.CHECKING) {
                return Mono.error(new RuntimeException("Los clientes PYME solo pueden abrir cuentas corrientes."));
            }
            account.setTransactionCommission(BigDecimal.ZERO);
            return hasCreditCard(customer.getId())
                    .flatMap(hasCard -> hasCard ? Mono.empty() : Mono.error(new RuntimeException("El cliente PYME debe tener una tarjeta de crédito.")));
        }

        return Mono.empty();
    }

    private Mono<Boolean> hasCreditCard(String customerId) {
        return creditClient.getCreditsByCustomer(customerId)
                .any(credit -> credit.getCreditType() == CreditType.CREDIT_CARD);
    }

    // Fixed-term account validation
    private Mono<Void> validateFixedTermAccount(Account account) {
        if (account.getType() != AccountType.FIXED_TERM) {
            return Mono.empty();
        }
        int today = LocalDate.now().getDayOfMonth();
        if (!Integer.valueOf(today).equals(account.getMovementDay())) {
            return Mono.error(new RuntimeException("Las operaciones solo están permitidas durante el día " + account.getMovementDay()));
        }
        return Mono.empty();
    }

    // Transfer helpers
    private Mono<Tuple2<Account, Account>> validateTransfer(Tuple2<Account, Account> accounts, BigDecimal amount) {
        Account from = accounts.getT1();
        Account to = accounts.getT2();
        if (from.getBalance().compareTo(amount) < 0) {
            return Mono.error(new RuntimeException("Fondos insuficientes en la cuenta de origen."));
        }
        return validateFixedTermAccount(from).then(validateFixedTermAccount(to)).thenReturn(accounts);
    }


    private Mono<Void> executeTransfer(Tuple2<Account, Account> accounts, BigDecimal amount) {
        Account from = accounts.getT1();
        Account to = accounts.getT2();
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        from.setTransactionCount(from.getTransactionCount() + 1);
        to.setTransactionCount(to.getTransactionCount() + 1);

        return repository.save(from)
                .flatMap(savedFrom -> repository.save(to)
                        .flatMap(savedTo -> Mono.when(
                                saveMovement(savedFrom, MovementType.WITHDRAW, amount),
                                saveMovement(savedTo, MovementType.DEPOSIT, amount)
                        ).then(
                                Mono.when(
                                        redisTemplate.delete("account:" + savedFrom.getId()),
                                        redisTemplate.delete("account:" + savedTo.getId())
                                )
                        ))
                ).then();
    }

    // Movements & commissions
    private Mono<Void> saveMovement(Account account, MovementType movementType, BigDecimal amount) {
        AccountMovementEvent event = AccountMovementEvent.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getType())
                .customerId(account.getCustomerId())
                .movementType(movementType)
                .amount(amount)
                .balanceAfterMovement(account.getBalance())
                .movementDate(LocalDateTime.now())
                .build();

        return accountMovementProducer.send(event);
    }

    private Mono<Account> applyTransactionCommission(Account account) {
        if (account.getFreeTransactions() == null || account.getTransactionCommission() == null) {
            return Mono.just(account);
        }
        if (account.getTransactionCount() >= account.getFreeTransactions()) {
            BigDecimal balanceAfterCommission = account.getBalance().subtract(account.getTransactionCommission());
            if (balanceAfterCommission.compareTo(BigDecimal.ZERO) < 0) {
                return Mono.error(new RuntimeException("Insufficient balance to pay transaction commission")
                );
            }
            account.setBalance(balanceAfterCommission);
        }
        return Mono.just(account);
    }
}

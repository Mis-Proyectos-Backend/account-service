package com.bank.account.service.impl;

import com.bank.account.client.CreditClient;
import com.bank.account.client.CustomerClient;
import com.bank.account.client.dto.Credit;
import com.bank.account.client.dto.Customer;
import com.bank.account.client.dto.WithdrawRequest;
import com.bank.account.config.AccountProperties;
import com.bank.account.enums.*;
import com.bank.account.event.AccountMovementEvent;
import com.bank.account.model.Account;
import com.bank.account.producer.AccountMovementProducer;
import com.bank.account.repository.AccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {


    @Mock
    AccountRepository repository;


    @Mock
    CustomerClient customerClient;


    @Mock
    CreditClient creditClient;


    @Mock
    AccountMovementProducer movementProducer;


    @Mock
    AccountProperties accountProperties;


    @Mock
    AccountProperties.AccountConfig accountConfig;

    @Mock
    ReactiveRedisTemplate<String, Account> redisTemplate;

    @Mock
    ReactiveValueOperations<String, Account> valueOperations;

    private AccountServiceImpl service;


    @BeforeEach
    void setUp() {

        service = new AccountServiceImpl(
                repository,
                customerClient,
                creditClient,
                movementProducer,
                accountProperties,
                redisTemplate
        );

        lenient()
                .when(accountConfig.getFreeTransactions())
                .thenReturn(5);

        lenient()
                .when(accountConfig.getTransactionCommission())
                .thenReturn(BigDecimal.valueOf(2));

        lenient()
                .when(accountProperties.getByType(any()))
                .thenReturn(accountConfig);

        lenient()
                .when(movementProducer.send(any(AccountMovementEvent.class)))
                .thenReturn(Mono.empty());

        lenient()
                .when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        lenient()
                .when(valueOperations.set(anyString(), any(Account.class)))
                .thenReturn(Mono.just(true));

        lenient()
                .when(redisTemplate.delete(anyString()))
                .thenReturn(Mono.just(1L));

        lenient()
                .when(valueOperations.get(anyString()))
                .thenReturn(Mono.empty());
    }


    // ==================== CREATE TESTS ====================


    @Test
    void create_whenCustomerNotFound_shouldReturnNotFound() {
        Account account =
                Account.builder()
                        .customerId("c1")
                        .build();
        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.empty());
        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
        verify(customerClient)
                .getCustomerById("c1");
    }

    @Test
    void create_withPersonalCustomerAndCheckingAccount_shouldSucceed() {
        Customer customer =
                Customer.builder()
                        .id("c1")
                        .customerType(CustomerType.PERSONAL)
                        .customerProfile(CustomerProfile.STANDARD)
                        .build();
        Account account =
                Account.builder()
                        .customerId("c1")
                        .type(AccountType.CHECKING)
                        .build();
        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));
        when(repository.existsByCustomerIdAndType(
                "c1",
                AccountType.CHECKING))
                .thenReturn(Mono.just(false));
        when(repository.save(any(Account.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        when(creditClient.hasOverdueDebt("c1"))
                .thenReturn(Mono.just(false));

        StepVerifier.create(service.create(account))
                .expectNextCount(1)
                .verifyComplete();
        verify(repository)
                .save(any(Account.class));
    }



    @Test
    void create_withBusinessCustomerAndSavingsAccount_shouldFail() {

        Customer customer =
                Customer.builder()
                        .id("c1")
                        .customerType(CustomerType.BUSINESS)
                        .build();

        Account account =
                Account.builder()
                        .customerId("c1")
                        .type(AccountType.SAVINGS)
                        .build();

        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }




    @Test
    void create_withVIPCustomerAndSavingsAccount_shouldSucceed() {
        Credit credit =
                Credit.builder()
                        .creditType(CreditType.CREDIT_CARD)
                        .build();
        Customer customer =
                Customer.builder()
                        .id("c1")
                        .customerType(CustomerType.PERSONAL)
                        .customerProfile(CustomerProfile.VIP)
                        .build();
        Account account =
                Account.builder()
                        .customerId("c1")
                        .type(AccountType.SAVINGS)
                        .build();
        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));

        when(creditClient.getCreditsByCustomer("c1"))
                .thenReturn(Flux.just(credit));

        when(repository.existsByCustomerIdAndType(
                "c1",
                AccountType.SAVINGS))
                .thenReturn(Mono.just(false));

        when(repository.save(any(Account.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        when(creditClient.hasOverdueDebt("c1"))
                .thenReturn(Mono.just(false));

        StepVerifier.create(service.create(account))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void create_withVIPCustomerWithoutCreditCard_shouldFail() {
        Customer customer =
                Customer.builder()
                        .id("c1")
                        .customerType(CustomerType.PERSONAL)
                        .customerProfile(CustomerProfile.VIP)
                        .build();

        Account account =
                Account.builder()
                        .customerId("c1")
                        .type(AccountType.SAVINGS)
                        .build();

        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    // ==================== DEPOSIT TESTS ====================


    @Test
    void deposit_whenAmountIsZero_shouldFail() {
        StepVerifier.create(
                        service.deposit("a1", BigDecimal.ZERO)
                )
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void deposit_whenAmountIsNegative_shouldFail() {
        StepVerifier.create(
                        service.deposit("a1", BigDecimal.valueOf(-50))
                )
                .expectError(IllegalArgumentException.class)
                .verify();
    }



    @Test
    void deposit_whenAccountNotFound_shouldFail() {
        when(repository.findById("a1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        service.deposit("a1", BigDecimal.valueOf(50))
                )
                .expectError()
                .verify();
    }




    @Test
    void deposit_shouldIncreaseBalanceAndTransactionCount() {
        Account account =
                Account.builder()
                        .id("a1")
                        .balance(BigDecimal.valueOf(100))
                        .transactionCount(0)
                        .freeTransactions(5)
                        .transactionCommission(BigDecimal.valueOf(2))
                        .type(AccountType.CHECKING)
                        .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.just(account));

        when(repository.save(any(Account.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(
                        service.deposit("a1", BigDecimal.valueOf(50))
                )
                .expectNextMatches(saved ->
                        saved.getBalance()
                                .compareTo(BigDecimal.valueOf(150)) == 0
                                &&
                                saved.getTransactionCount() == 1
                )
                .verifyComplete();

        verify(movementProducer)
                .send(argThat(event ->
                        event.getAccountId().equals("a1")
                                &&
                                event.getMovementType() == MovementType.DEPOSIT
                                &&
                                event.getAmount()
                                        .compareTo(BigDecimal.valueOf(50)) == 0
                ));
    }





    // ==================== WITHDRAW TESTS ====================



    @Test
    void withdraw_whenAmountIsZero_shouldFail() {
        WithdrawRequest request = WithdrawRequest.builder()
                .amount(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .build();

        StepVerifier.create(
                        service.withdraw("a1", request)
                )
                .expectError(IllegalArgumentException.class)
                .verify();
    }





    @Test
    void withdraw_whenAccountNotFound_shouldFail() {

        WithdrawRequest request = WithdrawRequest.builder()
                .amount(BigDecimal.valueOf(50))
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        service.withdraw("a1", request)
                )
                .expectError()
                .verify();
    }





    @Test
    void withdraw_whenInsufficientBalance_shouldFail() {
        Account account =
                Account.builder()
                        .id("a1")
                        .balance(BigDecimal.valueOf(20))
                        .transactionCount(0)
                        .type(AccountType.CHECKING)
                        .build();

        WithdrawRequest request = WithdrawRequest.builder()
                .amount(BigDecimal.valueOf(50))
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.just(account));

        StepVerifier.create(
                        service.withdraw("a1", request)
                )
                .expectError()
                .verify();
    }



    // ==================== TRANSFER TESTS ====================

    @Test
    void transfer_whenAmountIsZero_shouldFail() {
        StepVerifier.create(
                        service.transfer(
                                "a1",
                                "a2",
                                BigDecimal.ZERO
                        )
                )
                .expectError(IllegalArgumentException.class)
                .verify();
    }





    @Test
    void transfer_whenSourceAccountNotFound_shouldFail() {
        when(repository.findById("a1"))
                .thenReturn(Mono.empty());

        when(repository.findById("a2"))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        service.transfer(
                                "a1",
                                "a2",
                                BigDecimal.valueOf(50)
                        )
                )
                .expectError()
                .verify();
    }

    @Test
    void transfer_whenDestinationAccountNotFound_shouldFail() {
        Account source =
                Account.builder()
                        .id("a1")
                        .balance(BigDecimal.valueOf(100))
                        .type(AccountType.CHECKING)
                        .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.just(source));

        when(repository.findById("a2"))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        service.transfer(
                                "a1",
                                "a2",
                                BigDecimal.valueOf(50)
                        )
                )
                .expectError()
                .verify();
    }





    @Test
    void transfer_whenInsufficientBalance_shouldFail() {
        Account source =
                Account.builder()
                        .id("a1")
                        .balance(BigDecimal.valueOf(20))
                        .type(AccountType.CHECKING)
                        .build();

        Account destination =
                Account.builder()
                        .id("a2")
                        .balance(BigDecimal.valueOf(100))
                        .type(AccountType.CHECKING)
                        .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.just(source));

        when(repository.findById("a2"))
                .thenReturn(Mono.just(destination));

        StepVerifier.create(
                        service.transfer(
                                "a1",
                                "a2",
                                BigDecimal.valueOf(50)
                        )
                )
                .expectError()
                .verify();
    }


    @Test
    void transfer_shouldUpdateBalancesAndPublishEvents() {

        Account source =
                Account.builder()
                        .id("a1")
                        .balance(BigDecimal.valueOf(100))
                        .transactionCount(0)
                        .type(AccountType.CHECKING)
                        .build();

        Account destination =
                Account.builder()
                        .id("a2")
                        .balance(BigDecimal.valueOf(50))
                        .transactionCount(0)
                        .type(AccountType.CHECKING)
                        .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.just(source));

        when(repository.findById("a2"))
                .thenReturn(Mono.just(destination));

        when(repository.save(any(Account.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(
                        service.transfer(
                                "a1",
                                "a2",
                                BigDecimal.valueOf(30)
                        )
                )
                .verifyComplete();

        verify(repository, times(2))
                .save(any(Account.class));

        verify(movementProducer, times(2))
                .send(any(AccountMovementEvent.class));
    }


    // ==================== COMMISSION TESTS ====================

    @Test
    void deposit_whenCommissionApplies_shouldDeductCommission() {
        Account account =
                Account.builder()
                        .id("a1")
                        .balance(BigDecimal.valueOf(100))
                        .transactionCount(5)
                        .freeTransactions(2)
                        .transactionCommission(BigDecimal.valueOf(5))
                        .type(AccountType.CHECKING)
                        .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.just(account));

        when(repository.save(any(Account.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(
                        service.deposit("a1", BigDecimal.valueOf(50))
                )
                .expectNextMatches(saved ->
                        saved.getBalance()
                                .compareTo(BigDecimal.valueOf(145)) == 0
                )
                .verifyComplete();

    }


    @Test
    void deposit_whenCommissionCannotBePaid_shouldFail() {
        Account account =
                Account.builder()
                        .id("a1")
                        .balance(BigDecimal.valueOf(3))
                        .transactionCount(5)
                        .freeTransactions(2)
                        .transactionCommission(BigDecimal.valueOf(5))
                        .type(AccountType.CHECKING)
                        .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.just(account));

        StepVerifier.create(
                        service.deposit("a1", BigDecimal.valueOf(50))
                )
                .expectError()
                .verify();
    }

    // ==================== GET TESTS ====================


    @Test
    void getById_whenAccountExists_shouldReturnAccount() {
        Account account =
                Account.builder()
                        .id("a1")
                        .build();
        when(repository.findById("a1"))
                .thenReturn(Mono.just(account));

        when(valueOperations.get("account:a1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.getById("a1"))
                .expectNext(account)
                .verifyComplete();
    }

    @Test
    void getById_shouldReturnAccountFromCache() {
        Account account = Account.builder()
                .id("a1")
                .build();

        when(valueOperations.get("account:a1"))
                .thenReturn(Mono.just(account));
        // Necesario porque switchIfEmpty recibe este Mono ya construido
        when(repository.findById("a1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.getById("a1"))
                .expectNext(account)
                .verifyComplete();

        verify(valueOperations).get("account:a1");
    }

    @Test
    void getById_whenAccountNotFound_shouldThrowException() {
        when(valueOperations.get("account:a1"))
                .thenReturn(Mono.empty());
        when(repository.findById("a1"))
                .thenReturn(Mono.empty());
        StepVerifier.create(service.getById("a1"))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                ex.getMessage().equals("Cuenta no encontrada"))
                .verify();
    }

    @Test
    void getAll_shouldReturnAccounts() {
        Account account1 =
                Account.builder()
                        .id("a1")
                        .build();
        Account account2 =
                Account.builder()
                        .id("a2")
                        .build();

        when(repository.findAll())
                .thenReturn(Flux.just(account1, account2));

        StepVerifier.create(service.getAll())
                .expectNext(account1)
                .expectNext(account2)
                .verifyComplete();

    }







    @Test
    void getByCustomerId_shouldReturnCustomerAccounts() {


        Account account1 =
                Account.builder()
                        .id("a1")
                        .customerId("c1")
                        .build();


        Account account2 =
                Account.builder()
                        .id("a2")
                        .customerId("c1")
                        .build();



        when(repository.findByCustomerId("c1"))
                .thenReturn(Flux.just(account1, account2));



        StepVerifier.create(service.getByCustomerId("c1"))
                .expectNext(account1)
                .expectNext(account2)
                .verifyComplete();

    }

    // ==================== UPDATE TESTS ====================



    @Test
    void update_whenAccountNotFound_shouldFail() {
        when(repository.findById("a1"))
                .thenReturn(Mono.empty());
        StepVerifier.create(
                        service.update(
                                "a1",
                                Account.builder().build()
                        )
                )
                .expectError()
                .verify();
    }


    @Test
    void update_shouldUpdateAccountType() {
        Account existing =
                Account.builder()
                        .id("a1")
                        .type(AccountType.CHECKING)
                        .build();

        Account updated =
                Account.builder()
                        .type(AccountType.SAVINGS)
                        .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.just(existing));

        when(repository.save(any(Account.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(
                        service.update("a1", updated)
                )
                .expectNextMatches(account ->
                        account.getType() == AccountType.SAVINGS
                )
                .verifyComplete();
    }


    // ==================== DELETE TESTS ====================


    @Test
    void delete_whenAccountNotFound_shouldFail() {
        when(repository.findById("a1"))
                .thenReturn(Mono.empty());
        StepVerifier.create(service.delete("a1"))
                .expectError()
                .verify();
    }


    @Test
    void delete_shouldDeleteAccount() {
        Account account =
                Account.builder()
                        .id("a1")
                        .build();
        when(repository.findById("a1"))
                .thenReturn(Mono.just(account));
        when(repository.delete(account))
                .thenReturn(Mono.empty());
        StepVerifier.create(service.delete("a1"))
                .verifyComplete();
        verify(repository)
                .delete(account);

    }


    // ==================== FIXED TERM ACCOUNT ====================


    @Test
    void deposit_fixedTermAccount_wrongDay_shouldFail() {
        Account account =
                Account.builder()
                        .id("a1")
                        .type(AccountType.FIXED_TERM)
                        .movementDay(1)
                        .balance(BigDecimal.valueOf(100))
                        .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.just(account));

        StepVerifier.create(
                        service.deposit(
                                "a1",
                                BigDecimal.valueOf(50)
                        )
                )
                .expectError()
                .verify();

    }

    @Test
    void withdraw_fixedTermAccount_wrongDay_shouldFail() {
        Account account =
                Account.builder()
                        .id("a1")
                        .type(AccountType.FIXED_TERM)
                        .movementDay(1)
                        .balance(BigDecimal.valueOf(100))
                        .build();
        WithdrawRequest request = WithdrawRequest.builder()
                .amount(BigDecimal.valueOf(50))
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .build();

        when(repository.findById("a1"))
                .thenReturn(Mono.just(account));

        StepVerifier.create(
                        service.withdraw(
                                "a1",request
                        )
                )
                .expectError()
                .verify();

    }


    // ==================== CUSTOMER RULES ====================


    @Test
    void create_withBusinessCustomerAndFixedTerm_shouldFail() {

        Customer customer =
                Customer.builder()
                        .id("c1")
                        .customerType(CustomerType.BUSINESS)
                        .build();

        Account account =
                Account.builder()
                        .customerId("c1")
                        .type(AccountType.FIXED_TERM)
                        .movementDay(15)
                        .build();

        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();

    }

    @Test
    void create_withPYMECustomerAndCheckingWithCard_shouldSucceed() {
        Credit credit = Credit.builder()
                .creditType(CreditType.CREDIT_CARD)
                .build();

        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .customerProfile(CustomerProfile.PYME)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.CHECKING)
                .build();

        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));

        // NUEVO
        when(creditClient.hasOverdueDebt("c1"))
                .thenReturn(Mono.just(false));

        when(creditClient.getCreditsByCustomer("c1"))
                .thenReturn(Flux.just(credit));

        when(repository.existsByCustomerIdAndType(
                "c1",
                AccountType.CHECKING))
                .thenReturn(Mono.just(false));

        when(repository.save(any(Account.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(service.create(account))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void create_withPYMEWithoutCreditCard_shouldFail() {

        Customer customer =
                Customer.builder()
                        .id("c1")
                        .customerType(CustomerType.PERSONAL)
                        .customerProfile(CustomerProfile.PYME)
                        .build();

        Account account =
                Account.builder()
                        .customerId("c1")
                        .type(AccountType.CHECKING)
                        .build();

        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    @Test
    void create_whenCustomerHasOverdueDebt_shouldFail() {

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.SAVINGS)
                .build();

        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .build();

        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));

        when(creditClient.hasOverdueDebt("c1"))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.create(account))
                .expectErrorMessage("Customer has overdue credit debt")
                .verify();

        verify(repository, never()).save(any());
    }
}

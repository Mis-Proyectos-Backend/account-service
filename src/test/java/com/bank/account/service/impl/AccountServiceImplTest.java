package com.bank.account.service.impl;

import com.bank.account.client.CreditClient;
import com.bank.account.client.CustomerClient;
import com.bank.account.client.dto.Credit;
import com.bank.account.client.dto.Customer;
import com.bank.account.config.AccountProperties;
import com.bank.account.enums.*;
import com.bank.account.model.Account;
import com.bank.account.model.Movement;
import com.bank.account.repository.AccountRepository;
import com.bank.account.service.MovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.mockito.InjectMocks;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    AccountRepository repository;

    @Mock
    CustomerClient customerClient;

    @Mock
    CreditClient creditClient;

    @Mock
    MovementService movementService;

    @Mock
    AccountProperties accountProperties;

    @Mock
    AccountProperties.AccountConfig accountConfig;

    @InjectMocks
    AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(accountConfig.getFreeTransactions()).thenReturn(5);
        lenient().when(accountConfig.getTransactionCommission()).thenReturn(BigDecimal.valueOf(2));
        lenient().when(accountProperties.getByType(any())).thenReturn(accountConfig);
    }

    // ==================== CREATE TESTS ====================

    @Test
    void create_whenCustomerNotFound_shouldReturnNotFound() {
        Account account = Account.builder().customerId("c1").build();
        when(customerClient.getCustomerById("c1")).thenReturn(Mono.empty());

        StepVerifier.create(service.create(account))
                .expectErrorMatches(throwable -> throwable instanceof org.springframework.web.server.ResponseStatusException)
                .verify();

        verify(customerClient).getCustomerById("c1");
        verifyNoMoreInteractions(repository);
    }

    // ==================== DEPOSIT TESTS ====================

    @Test
    void deposit_whenAmountIsZero_shouldThrowIllegalArgumentException() {
        StepVerifier.create(service.deposit("a1", BigDecimal.ZERO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void deposit_whenAmountIsNegative_shouldThrowIllegalArgumentException() {
        StepVerifier.create(service.deposit("a1", BigDecimal.valueOf(-50)))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void deposit_whenAccountNotFound_shouldThrowException() {
        when(repository.findById("a1")).thenReturn(Mono.empty());

        StepVerifier.create(service.deposit("a1", BigDecimal.valueOf(50)))
                .expectError()
                .verify();
    }

    @Test
    void deposit_whenSuccessful_shouldIncreaseBalance() {
        Account account = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(100))
                .freeTransactions(5)
                .transactionCommission(BigDecimal.valueOf(2))
                .type(AccountType.CHECKING)
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(account));
        when(movementService.countMovements("a1")).thenReturn(Mono.just(0L));
        when(repository.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(movementService.save(any(Movement.class))).thenReturn(Mono.just(Movement.builder().id("m1").build()));

        StepVerifier.create(service.deposit("a1", BigDecimal.valueOf(50)))
                .expectNextMatches(saved -> saved.getBalance().compareTo(BigDecimal.valueOf(150)) == 0)
                .verifyComplete();

        verify(repository).save(argThat(acc -> acc.getBalance().compareTo(BigDecimal.valueOf(150)) == 0));
    }

    @Test
    void deposit_shouldCreateMovement() {
        Account account = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(100))
                .freeTransactions(5)
                .transactionCommission(BigDecimal.valueOf(2))
                .type(AccountType.CHECKING)
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(account));
        when(movementService.countMovements("a1")).thenReturn(Mono.just(0L));
        when(repository.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(movementService.save(any(Movement.class))).thenReturn(Mono.just(Movement.builder().id("m1").build()));

        StepVerifier.create(service.deposit("a1", BigDecimal.valueOf(50)))
                .expectNextCount(1)
                .verifyComplete();

        verify(movementService).save(argThat(m -> m.getMovementType() == MovementType.DEPOSIT
                && m.getAmount().compareTo(BigDecimal.valueOf(50)) == 0
                && m.getAccountId().equals("a1")));
    }

    // ==================== WITHDRAW TESTS ====================

    @Test
    void withdraw_whenAmountIsZero_shouldThrowIllegalArgumentException() {
        StepVerifier.create(service.withdraw("a1", BigDecimal.ZERO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void withdraw_whenAccountNotFound_shouldThrowException() {
        when(repository.findById("a1")).thenReturn(Mono.empty());

        StepVerifier.create(service.withdraw("a1", BigDecimal.valueOf(50)))
                .expectError()
                .verify();
    }

    @Test
    void withdraw_whenInsufficientBalance_shouldThrowException() {
        Account account = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(30))
                .freeTransactions(5)
                .transactionCommission(BigDecimal.valueOf(2))
                .type(AccountType.CHECKING)
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(account));
        when(movementService.countMovements("a1")).thenReturn(Mono.just(0L));

        StepVerifier.create(service.withdraw("a1", BigDecimal.valueOf(50)))
                .expectError()
                .verify();
    }

    @Test
    void withdraw_whenSuccessful_shouldDecreaseBalance() {
        Account account = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(100))
                .freeTransactions(5)
                .transactionCommission(BigDecimal.valueOf(2))
                .type(AccountType.CHECKING)
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(account));
        when(movementService.countMovements("a1")).thenReturn(Mono.just(0L));
        when(repository.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(movementService.save(any(Movement.class))).thenReturn(Mono.just(Movement.builder().id("m1").build()));

        StepVerifier.create(service.withdraw("a1", BigDecimal.valueOf(30)))
                .expectNextMatches(saved -> saved.getBalance().compareTo(BigDecimal.valueOf(70)) == 0)
                .verifyComplete();

        verify(repository).save(argThat(acc -> acc.getBalance().compareTo(BigDecimal.valueOf(70)) == 0));
    }

    // ==================== TRANSFER TESTS ====================

    @Test
    void transfer_whenAmountIsZero_shouldThrowIllegalArgumentException() {
        StepVerifier.create(service.transfer("a1", "a2", BigDecimal.ZERO))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void transfer_whenSourceAccountNotFound_shouldThrowException() {
        when(repository.findById("a1")).thenReturn(Mono.empty());
        when(repository.findById("a2")).thenReturn(Mono.empty());

        StepVerifier.create(service.transfer("a1", "a2", BigDecimal.valueOf(50)))
                .expectError()
                .verify();
    }

    @Test
    void transfer_whenDestinationAccountNotFound_shouldThrowException() {
        Account source = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(100))
                .type(AccountType.CHECKING)
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(source));
        when(repository.findById("a2")).thenReturn(Mono.empty());

        StepVerifier.create(service.transfer("a1", "a2", BigDecimal.valueOf(50)))
                .expectError()
                .verify();
    }

    @Test
    void transfer_whenInsufficientBalance_shouldThrowException() {
        Account source = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(30))
                .type(AccountType.CHECKING)
                .build();

        Account dest = Account.builder()
                .id("a2")
                .balance(BigDecimal.valueOf(100))
                .type(AccountType.CHECKING)
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(source));
        when(repository.findById("a2")).thenReturn(Mono.just(dest));

        StepVerifier.create(service.transfer("a1", "a2", BigDecimal.valueOf(50)))
                .expectError()
                .verify();
    }

    @Test
    void transfer_whenSuccessful_shouldUpdateBothBalances() {
        Account source = Account.builder()
                .id("a1")
                .balance(BigDecimal.valueOf(100))
                .type(AccountType.CHECKING)
                .build();

        Account dest = Account.builder()
                .id("a2")
                .balance(BigDecimal.valueOf(50))
                .type(AccountType.CHECKING)
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(source));
        when(repository.findById("a2")).thenReturn(Mono.just(dest));
        when(repository.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(movementService.save(any(Movement.class))).thenReturn(Mono.just(Movement.builder().id("m1").build()));

        StepVerifier.create(service.transfer("a1", "a2", BigDecimal.valueOf(30)))
                .verifyComplete();

        verify(repository, times(2)).save(any(Account.class));
    }

    // ==================== GET TESTS ====================

    @Test
    void getById_whenAccountExists_shouldReturnAccount() {
        Account account = Account.builder().id("a1").build();
        when(repository.findById("a1")).thenReturn(Mono.just(account));

        StepVerifier.create(service.getById("a1"))
                .expectNext(account)
                .verifyComplete();
    }

    @Test
    void getById_whenAccountNotFound_shouldReturnEmpty() {
        when(repository.findById("a1")).thenReturn(Mono.empty());

        StepVerifier.create(service.getById("a1"))
                .expectComplete()
                .verify();
    }

    @Test
    void getAll_shouldReturnAllAccounts() {
        Account a1 = Account.builder().id("a1").build();
        Account a2 = Account.builder().id("a2").build();

        when(repository.findAll()).thenReturn(Flux.just(a1, a2));

        StepVerifier.create(service.getAll())
                .expectNext(a1, a2)
                .verifyComplete();
    }

    @Test
    void getByCustomerId_shouldReturnAccountsForCustomer() {
        Account a1 = Account.builder().id("a1").customerId("c1").build();
        Account a2 = Account.builder().id("a2").customerId("c1").build();

        when(repository.findByCustomerId("c1")).thenReturn(Flux.just(a1, a2));

        StepVerifier.create(service.getByCustomerId("c1"))
                .expectNext(a1, a2)
                .verifyComplete();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    void update_whenAccountNotFound_shouldThrowException() {
        when(repository.findById("a1")).thenReturn(Mono.empty());

        StepVerifier.create(service.update("a1", Account.builder().build()))
                .expectError()
                .verify();
    }

    @Test
    void update_whenSuccessful_shouldUpdateAccountType() {
        Account existing = Account.builder()
                .id("a1")
                .type(AccountType.CHECKING)
                .build();

        Account updated = Account.builder()
                .type(AccountType.SAVINGS)
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(existing));
        when(repository.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(service.update("a1", updated))
                .expectNextMatches(acc -> acc.getType() == AccountType.SAVINGS)
                .verifyComplete();
    }

    // ==================== DELETE TESTS ====================

    @Test
    void delete_whenAccountNotFound_shouldThrowException() {
        when(repository.findById("a1")).thenReturn(Mono.empty());

        StepVerifier.create(service.delete("a1"))
                .expectError()
                .verify();
    }

    @Test
    void delete_whenSuccessful_shouldDeleteAccount() {
        Account account = Account.builder().id("a1").build();
        when(repository.findById("a1")).thenReturn(Mono.just(account));
        when(repository.delete(account)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete("a1"))
                .verifyComplete();

        verify(repository).delete(account);
    }

    // ==================== CREATE - PERSONAL CUSTOMER TESTS ====================

    @Test
    void create_withPersonalCustomerAndCheckingAccount_shouldSucceed() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .customerProfile(CustomerProfile.STANDARD)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.CHECKING)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));
        when(repository.existsByCustomerIdAndType("c1", AccountType.CHECKING)).thenReturn(Mono.just(false));
        when(repository.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(service.create(account))
                .expectNextCount(1)
                .verifyComplete();

        verify(repository).save(argThat(acc -> acc.getType() == AccountType.CHECKING));
    }

    @Test
    void create_withPersonalCustomerWhenAccountExists_shouldFail() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .customerProfile(CustomerProfile.STANDARD)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.CHECKING)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));
        when(repository.existsByCustomerIdAndType("c1", AccountType.CHECKING)).thenReturn(Mono.just(true));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    @Test
    void create_withPersonalCustomerAndFixedTermAccount_withInvalidMovementDay_shouldFail() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.FIXED_TERM)
                .movementDay(32)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    @Test
    void create_withPersonalCustomerAndFixedTermAccount_withValidMovementDay_shouldSucceed() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .customerProfile(CustomerProfile.STANDARD)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.FIXED_TERM)
                .movementDay(15)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));
        when(repository.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(service.create(account))
                .expectNextCount(1)
                .verifyComplete();

        verify(repository).save(any(Account.class));
    }

    @Test
    void create_withBusinessCustomer_shouldSucceed() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.BUSINESS)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.CHECKING)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));
        when(repository.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(service.create(account))
                .expectNextCount(1)
                .verifyComplete();

        verify(repository).save(any(Account.class));
    }

    @Test
    void create_withBusinessCustomerAndSavingsAccount_shouldFail() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.BUSINESS)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.SAVINGS)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    @Test
    void create_withBusinessCustomerAndFixedTermAccount_shouldFail() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.BUSINESS)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.FIXED_TERM)
                .movementDay(15)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    @Test
    void create_withInvalidCustomerType_shouldFail() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(null)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.CHECKING)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    @Test
    void create_withNegativeInitialBalance_shouldFail() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .customerProfile(CustomerProfile.STANDARD)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.CHECKING)
                .balance(BigDecimal.valueOf(-100))
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    @Test
    void create_withVIPCustomerAndSavingsAccount_shouldSucceed() {

        Credit creditCard = Credit.builder()
                .creditType(CreditType.CREDIT_CARD)
                .build();

        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .customerProfile(CustomerProfile.VIP)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.SAVINGS)
                .build();


        when(customerClient.getCustomerById("c1"))
                .thenReturn(Mono.just(customer));

        when(creditClient.getCreditsByCustomer("c1"))
                .thenReturn(Flux.just(creditCard));

        when(repository.existsByCustomerIdAndType("c1", AccountType.SAVINGS))
                .thenReturn(Mono.just(false));

        when(repository.save(any(Account.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));


        StepVerifier.create(service.create(account))
                .expectNextCount(1)
                .verifyComplete();


        verify(repository).save(any(Account.class));
    }

    @Test
    void create_withVIPCustomerWithoutCreditCard_shouldFail() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .customerProfile(CustomerProfile.VIP)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.SAVINGS)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    @Test
    void create_withVIPCustomerAndCheckingAccount_shouldFail() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .customerProfile(CustomerProfile.VIP)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.CHECKING)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    @Test
    void create_withPYMECustomerAndCheckingAccount_shouldSucceed() {

        Credit creditCard = Credit.builder()
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

        when(creditClient.getCreditsByCustomer("c1"))
                .thenReturn(Flux.just(creditCard));

        when(repository.existsByCustomerIdAndType("c1", AccountType.CHECKING))
                .thenReturn(Mono.just(false));

        when(repository.save(any(Account.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.create(account))
                .expectNextCount(1)
                .verifyComplete();

        verify(repository).save(any(Account.class));
    }

    @Test
    void create_withPYMECustomerWithoutCreditCard_shouldFail() {

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

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    @Test
    void create_withPYMECustomerAndSavingsAccount_shouldFail() {
        Customer customer = Customer.builder()
                .id("c1")
                .customerType(CustomerType.PERSONAL)
                .customerProfile(CustomerProfile.PYME)
                .build();

        Account account = Account.builder()
                .customerId("c1")
                .type(AccountType.SAVINGS)
                .build();

        when(customerClient.getCustomerById("c1")).thenReturn(Mono.just(customer));

        StepVerifier.create(service.create(account))
                .expectError()
                .verify();
    }

    // ==================== FIXED TERM ACCOUNT VALIDATION ====================

    @Test
    void deposit_onFixedTermAccount_whenNotMovementDay_shouldFail() {
        Account account = Account.builder()
                .id("a1")
                .type(AccountType.FIXED_TERM)
                .movementDay(25)
                .balance(BigDecimal.valueOf(100))
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(account));

        StepVerifier.create(service.deposit("a1", BigDecimal.valueOf(50)))
                .expectError()
                .verify();
    }

    @Test
    void withdraw_onFixedTermAccount_whenNotMovementDay_shouldFail() {
        Account account = Account.builder()
                .id("a1")
                .type(AccountType.FIXED_TERM)
                .movementDay(25)
                .balance(BigDecimal.valueOf(100))
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(account));

        StepVerifier.create(service.withdraw("a1", BigDecimal.valueOf(50)))
                .expectError()
                .verify();
    }

    // ==================== COMMISSION TESTS ====================

    @Test
    void deposit_whenCommissionApplies_shouldDeductFromBalance() {
        Account account = Account.builder()
                .id("a1")
                .type(AccountType.CHECKING)
                .balance(BigDecimal.valueOf(100))
                .freeTransactions(2)
                .transactionCommission(BigDecimal.valueOf(5))
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(account));
        when(movementService.countMovements("a1")).thenReturn(Mono.just(3L));
        when(repository.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(movementService.save(any(Movement.class))).thenReturn(Mono.just(Movement.builder().id("m1").build()));

        StepVerifier.create(service.deposit("a1", BigDecimal.valueOf(50)))
                .expectNextMatches(saved -> saved.getBalance().compareTo(BigDecimal.valueOf(145)) == 0)
                .verifyComplete();
    }

    @Test
    void deposit_whenCommissionExceedsBalance_shouldFail() {
        Account account = Account.builder()
                .id("a1")
                .type(AccountType.CHECKING)
                .balance(BigDecimal.valueOf(3))
                .freeTransactions(2)
                .transactionCommission(BigDecimal.valueOf(5))
                .build();

        when(repository.findById("a1")).thenReturn(Mono.just(account));
        when(movementService.countMovements("a1")).thenReturn(Mono.just(3L));

        StepVerifier.create(service.deposit("a1", BigDecimal.valueOf(50)))
                .expectError()
                .verify();
    }
}

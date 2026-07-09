package com.bank.account.service.impl;

import com.bank.account.dto.ProductReportResponse;
import com.bank.account.enums.AccountType;
import com.bank.account.enums.MovementType;
import com.bank.account.model.Account;
import com.bank.account.model.Movement;
import com.bank.account.repository.AccountRepository;
import com.bank.account.repository.MovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    MovementRepository movementRepository;

    @InjectMocks
    ReportServiceImpl service;

    @Test
    void generateAccountReport_withNoMovements_shouldReturnReportWithZeroTotals() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

        Account account = Account.builder()
                .id("a1")
                .accountNumber("ACC001")
                .type(AccountType.CHECKING)
                .balance(BigDecimal.valueOf(1000))
                .build();

        when(accountRepository.findById("a1")).thenReturn(Mono.just(account));
        when(movementRepository.findByAccountIdAndMovementDateBetweenOrderByMovementDateDesc("a1", start, end))
                .thenReturn(Flux.empty());

        StepVerifier.create(service.generateAccountReport("a1", start, end))
                .expectNextMatches(report ->
                        report.getAccountId().equals("a1") &&
                        report.getTotalDeposits().compareTo(BigDecimal.ZERO) == 0 &&
                        report.getTotalWithdrawals().compareTo(BigDecimal.ZERO) == 0 &&
                        report.getTotalMovements() == 0
                )
                .verifyComplete();

        verify(accountRepository).findById("a1");
        verify(movementRepository).findByAccountIdAndMovementDateBetweenOrderByMovementDateDesc("a1", start, end);
    }

    @Test
    void generateAccountReport_withDepositsAndWithdrawals_shouldCalculateTotals() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

        Account account = Account.builder()
                .id("a1")
                .accountNumber("ACC001")
                .type(AccountType.CHECKING)
                .balance(BigDecimal.valueOf(1500))
                .build();

        Movement m1 = Movement.builder()
                .id("m1")
                .movementType(MovementType.DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .build();

        Movement m2 = Movement.builder()
                .id("m2")
                .movementType(MovementType.WITHDRAW)
                .amount(BigDecimal.valueOf(300))
                .build();

        Movement m3 = Movement.builder()
                .id("m3")
                .movementType(MovementType.DEPOSIT)
                .amount(BigDecimal.valueOf(200))
                .build();

        when(accountRepository.findById("a1")).thenReturn(Mono.just(account));
        when(movementRepository.findByAccountIdAndMovementDateBetweenOrderByMovementDateDesc("a1", start, end))
                .thenReturn(Flux.just(m1, m2, m3));

        StepVerifier.create(service.generateAccountReport("a1", start, end))
                .expectNextMatches(report ->
                        report.getAccountId().equals("a1") &&
                        report.getTotalDeposits().compareTo(BigDecimal.valueOf(700)) == 0 &&
                        report.getTotalWithdrawals().compareTo(BigDecimal.valueOf(300)) == 0 &&
                        report.getTotalMovements() == 3 &&
                        report.getCurrentBalance().compareTo(BigDecimal.valueOf(1500)) == 0 &&
                        report.getAccountNumber().equals("ACC001") &&
                        report.getAccountType() == AccountType.CHECKING
                )
                .verifyComplete();
    }

    @Test
    void generateAccountReport_whenAccountNotFound_shouldReturnEmpty() {

        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

        when(accountRepository.findById("a1"))
                .thenReturn(Mono.empty());

        when(movementRepository
                .findByAccountIdAndMovementDateBetweenOrderByMovementDateDesc(
                        "a1", start, end))
                .thenReturn(Flux.empty());

        StepVerifier.create(service.generateAccountReport("a1", start, end))
                .verifyComplete();
    }

    @Test
    void getLast10Movements_shouldReturnFluxOfMovements() {
        Movement m1 = Movement.builder().id("m1").build();
        Movement m2 = Movement.builder().id("m2").build();

        when(movementRepository.findTop10ByAccountIdOrderByMovementDateDesc("a1"))
                .thenReturn(Flux.just(m1, m2));

        StepVerifier.create(service.getLast10Movements("a1"))
                .expectNext(m1, m2)
                .verifyComplete();

        verify(movementRepository).findTop10ByAccountIdOrderByMovementDateDesc("a1");
    }

    @Test
    void getLast10Movements_whenNoMovements_shouldReturnEmptyFlux() {
        when(movementRepository.findTop10ByAccountIdOrderByMovementDateDesc("a1"))
                .thenReturn(Flux.empty());

        StepVerifier.create(service.getLast10Movements("a1"))
                .expectComplete()
                .verify();

        verify(movementRepository).findTop10ByAccountIdOrderByMovementDateDesc("a1");
    }
}

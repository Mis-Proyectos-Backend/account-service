package com.bank.account.service.impl;

import com.bank.account.enums.MovementType;
import com.bank.account.model.Movement;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovementServiceImplTest {

    @Mock
    MovementRepository repository;

    @InjectMocks
    MovementServiceImpl service;

    @Test
    void save_shouldSaveMovement() {
        Movement movement = Movement.builder()
                .accountId("a1")
                .movementType(MovementType.DEPOSIT)
                .amount(BigDecimal.valueOf(100))
                .build();

        Movement saved = Movement.builder()
                .id("m1")
                .accountId("a1")
                .movementType(MovementType.DEPOSIT)
                .amount(BigDecimal.valueOf(100))
                .build();

        when(repository.save(movement)).thenReturn(Mono.just(saved));

        StepVerifier.create(service.save(movement))
                .expectNext(saved)
                .verifyComplete();

        verify(repository).save(movement);
    }

    @Test
    void findByAccount_shouldReturnMovementsForAccount() {
        Movement m1 = Movement.builder().id("m1").accountId("a1").build();
        Movement m2 = Movement.builder().id("m2").accountId("a1").build();

        when(repository.findByAccountId("a1")).thenReturn(Flux.just(m1, m2));

        StepVerifier.create(service.findByAccount("a1"))
                .expectNext(m1, m2)
                .verifyComplete();

        verify(repository).findByAccountId("a1");
    }

    @Test
    void countMovements_shouldReturnCount() {
        when(repository.countByAccountId("a1")).thenReturn(Mono.just(5L));

        StepVerifier.create(service.countMovements("a1"))
                .expectNext(5L)
                .verifyComplete();

        verify(repository).countByAccountId("a1");
    }

    @Test
    void getLast10Movements_shouldReturnLast10() {
        Movement m1 = Movement.builder().id("m1").build();
        Movement m2 = Movement.builder().id("m2").build();

        when(repository.findTop10ByAccountIdOrderByMovementDateDesc("a1"))
                .thenReturn(Flux.just(m1, m2));

        StepVerifier.create(service.getLast10Movements("a1"))
                .expectNext(m1, m2)
                .verifyComplete();

        verify(repository).findTop10ByAccountIdOrderByMovementDateDesc("a1");
    }

    @Test
    void findByAccountAndDateRange_shouldReturnMovementsBetweenDates() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);

        Movement m1 = Movement.builder().id("m1").build();
        Movement m2 = Movement.builder().id("m2").build();

        when(repository.findByAccountIdAndMovementDateBetweenOrderByMovementDateDesc("a1", start, end))
                .thenReturn(Flux.just(m1, m2));

        StepVerifier.create(service.findByAccountAndDateRange("a1", start, end))
                .expectNext(m1, m2)
                .verifyComplete();

        verify(repository).findByAccountIdAndMovementDateBetweenOrderByMovementDateDesc("a1", start, end);
    }

    @Test
    void findById_shouldReturnMovement() {
        Movement movement = Movement.builder()
                .id("m1")
                .accountId("a1")
                .movementType(MovementType.DEPOSIT)
                .build();

        when(repository.findById("m1")).thenReturn(Mono.just(movement));

        StepVerifier.create(service.findById("m1"))
                .expectNext(movement)
                .verifyComplete();

        verify(repository).findById("m1");
    }

    @Test
    void findById_whenNotFound_shouldReturnEmpty() {
        when(repository.findById("m1")).thenReturn(Mono.empty());

        StepVerifier.create(service.findById("m1"))
                .expectComplete()
                .verify();

        verify(repository).findById("m1");
    }
}

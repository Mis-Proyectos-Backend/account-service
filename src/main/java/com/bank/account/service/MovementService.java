package com.bank.account.service;

import com.bank.account.model.Movement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface MovementService {

    Mono<Movement> save(Movement movement);

    Flux<Movement> findByAccount(String accountId);

    Mono<Long> countMovements(String accountId);

    Flux<Movement> getLast10Movements(String accountId);

    Flux<Movement> findByAccountAndDateRange(
            String accountId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    Mono<Movement> findById(String id);
}

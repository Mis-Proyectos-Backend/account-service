package com.bank.account.repository;

import com.bank.account.model.Movement;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface MovementRepository extends ReactiveMongoRepository<Movement, String> {

    Flux<Movement> findByAccountId(String accountId);

    Mono<Long> countByAccountId(String accountId);

    Flux<Movement> findTop10ByAccountIdOrderByMovementDateDesc(String accountId);

    Flux<Movement> findByAccountIdAndMovementDateBetweenOrderByMovementDateDesc(
            String accountId,
            LocalDateTime startDate,
            LocalDateTime endDate);
}

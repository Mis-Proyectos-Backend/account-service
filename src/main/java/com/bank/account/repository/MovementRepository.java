package com.bank.account.repository;

import com.bank.account.model.Movement;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface MovementRepository extends ReactiveMongoRepository<Movement, String> {

    Flux<Movement> findByAccountId(String accountId);

}

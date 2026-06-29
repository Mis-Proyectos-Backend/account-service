package com.bank.account.service;

import com.bank.account.model.Movement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MovementService {

    Mono<Movement> save(Movement movement);

    Flux<Movement> findByAccount(String accountId);

}

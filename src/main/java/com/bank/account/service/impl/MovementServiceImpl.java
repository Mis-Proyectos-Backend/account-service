package com.bank.account.service.impl;

import com.bank.account.model.Movement;
import com.bank.account.repository.MovementRepository;
import com.bank.account.service.MovementService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class MovementServiceImpl implements MovementService {

    private final MovementRepository repository;

    public MovementServiceImpl(MovementRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Movement> save(Movement movement) {
        return repository.save(movement);
    }

    @Override
    public Flux<Movement> findByAccount(String accountId) {
        return repository.findByAccountId(accountId);
    }
}

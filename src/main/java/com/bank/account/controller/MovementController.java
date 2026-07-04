package com.bank.account.controller;

import com.bank.account.model.Movement;
import com.bank.account.service.MovementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/movements")
public class MovementController {

    private final MovementService service;

    public MovementController(MovementService service) {
        this.service = service;
    }

    @GetMapping("/account/{accountId}")
    public Flux<Movement> getMovements(
            @PathVariable String accountId) {

        return service.findByAccount(accountId);
    }

}
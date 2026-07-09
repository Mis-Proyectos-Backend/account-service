package com.bank.account.controller;

import com.bank.account.model.Movement;
import com.bank.account.service.MovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;


@RestController
@RequestMapping("/movements")
@RequiredArgsConstructor
public class MovementController {

    private final MovementService service;

    /**
     * Obtiene un movimiento por su id.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Movement>> findById(
            @PathVariable String id) {

        return service.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Obtiene todos los movimientos de una cuenta.
     */
    @GetMapping("/account/{accountId}")
    public Flux<Movement> findByAccount(
            @PathVariable String accountId) {

        return service.findByAccount(accountId);
    }

    /**
     * Obtiene los últimos 10 movimientos de una cuenta.
     */
    @GetMapping("/account/{accountId}/last10")
    public Flux<Movement> getLast10Movements(
            @PathVariable String accountId) {

        return service.getLast10Movements(accountId);
    }

    /**
     * Obtiene todos los movimientos entre un rango de fechas.
     */
    @GetMapping("/account/{accountId}/report")
    public Flux<Movement> getMovementsByDateRange(
            @PathVariable String accountId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {

        return service.findByAccountAndDateRange(
                accountId,
                startDate,
                endDate);
    }

}
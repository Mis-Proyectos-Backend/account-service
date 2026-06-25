package com.bank.account.controller;

import com.bank.account.model.Account;
import com.bank.account.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<Account> create(@RequestBody Account account) {
        return service.create(account);
    }

    @GetMapping("/{id}")
    public Mono<Account> getById(@PathVariable String id) {
        return service.getById(id);
    }

    @GetMapping
    public Flux<Account> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Account>> update(
            @PathVariable String id,
            @RequestBody Account account) {

        return service.update(id, account)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/deposit")
    public Mono<Account> deposit(@PathVariable String id,
                                 @RequestParam BigDecimal amount) {
        return service.deposit(id, amount);
    }

    @PostMapping("/{id}/withdraw")
    public Mono<Account> withdraw(@PathVariable String id,
                                  @RequestParam BigDecimal amount) {
        return service.withdraw(id, amount);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable String id) {
        return service.delete(id);
    }
}
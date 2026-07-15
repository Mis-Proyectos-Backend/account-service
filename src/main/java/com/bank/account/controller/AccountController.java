package com.bank.account.controller;

import com.bank.account.client.dto.WithdrawRequest;
import com.bank.account.dto.TransferRequest;
import com.bank.account.model.Account;
import com.bank.account.service.AccountService;
import jakarta.validation.Valid;
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
    public Mono<ResponseEntity<Account>> withdraw(
            @PathVariable String id,
            @Valid @RequestBody WithdrawRequest request) {

        return service.withdraw(id, request)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return service.delete(id)
                .thenReturn(ResponseEntity.ok().build());
    }


    @PostMapping("/transfer")
    public Mono<ResponseEntity<Void>> transfer(@RequestBody TransferRequest request) {
        return service.transfer(
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount()
        ).thenReturn(ResponseEntity.ok().build());
    }

    @GetMapping("/customer/{customerId}")
    public Flux<Account> getBycustomerId(@PathVariable String customerId) {
        return service.getByCustomerId(customerId);
    }

}
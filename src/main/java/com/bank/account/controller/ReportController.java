package com.bank.account.controller;

import com.bank.account.dto.ProductReportResponse;
import com.bank.account.model.Movement;
import com.bank.account.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    @GetMapping("/accounts/{accountId}")
    public Mono<ResponseEntity<ProductReportResponse>> generateReport(

            @PathVariable String accountId,

            @RequestParam LocalDateTime startDate,

            @RequestParam LocalDateTime endDate) {

        return service.generateAccountReport(
                        accountId,
                        startDate,
                        endDate)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/accounts/{accountId}/last10")
    public Flux<Movement> getLast10Movements(
            @PathVariable String accountId) {

        return service.getLast10Movements(accountId);
    }

}

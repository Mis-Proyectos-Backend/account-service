package com.bank.account.service;

import com.bank.account.dto.ProductReportResponse;
import com.bank.account.model.Movement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface ReportService {

    Mono<ProductReportResponse> generateAccountReport(
            String accountId,
            LocalDateTime startDate,
            LocalDateTime endDate);


    Flux<Movement> getLast10Movements(String accountId);

}
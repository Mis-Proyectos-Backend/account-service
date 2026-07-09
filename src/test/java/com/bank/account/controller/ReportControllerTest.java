package com.bank.account.controller;

import com.bank.account.dto.ProductReportResponse;
import com.bank.account.model.Movement;
import com.bank.account.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;


class ReportControllerTest {


    private ReportService service;
    private WebTestClient webTestClient;


    @BeforeEach
    void setUp() {

        service = Mockito.mock(ReportService.class);

        ReportController controller =
                new ReportController(service);

        webTestClient = WebTestClient
                .bindToController(controller)
                .build();
    }



    @Test
    void generateReport_shouldReturnReport() {


        LocalDateTime startDate =
                LocalDateTime.of(2026, 1, 1, 0, 0);

        LocalDateTime endDate =
                LocalDateTime.of(2026, 1, 31, 23, 59);


        ProductReportResponse response =
                ProductReportResponse.builder()
                        .accountId("a1")
                        .build();


        when(service.generateAccountReport(
                eq("a1"),
                eq(startDate),
                eq(endDate)
        ))
                .thenReturn(Mono.just(response));



        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reports/accounts/a1")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.accountId")
                .isEqualTo("a1");
    }



    @Test
    void getLast10Movements_shouldReturnMovements() {


        Movement movement1 = Movement.builder()
                .id("m1")
                .accountId("a1")
                .amount(BigDecimal.valueOf(100.0))
                .build();


        Movement movement2 = Movement.builder()
                .id("m2")
                .accountId("a1")
                .amount(BigDecimal.valueOf(50.0))
                .build();



        when(service.getLast10Movements("a1"))
                .thenReturn(
                        Flux.just(movement1, movement2)
                );



        webTestClient.get()
                .uri("/reports/accounts/a1/last10")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Movement.class)
                .hasSize(2);
    }

}
package com.bank.account.controller;

import com.bank.account.model.Movement;
import com.bank.account.service.MovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class MovementControllerTest {


    private MovementService service;
    private WebTestClient webTestClient;


    @BeforeEach
    void setUp() {

        service = Mockito.mock(MovementService.class);

        MovementController controller =
                new MovementController(service);

        webTestClient = WebTestClient
                .bindToController(controller)
                .build();
    }




    @Test
    void findById_shouldReturnMovement() {

        Movement movement = Movement.builder()
                .id("m1")
                .accountId("a1")
                .amount(BigDecimal.valueOf(100.0))
                .build();


        when(service.findById("m1"))
                .thenReturn(Mono.just(movement));


        webTestClient.get()
                .uri("/movements/m1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("m1")
                .jsonPath("$.accountId").isEqualTo("a1")
                .jsonPath("$.amount").isEqualTo(100.0);
    }


    @Test
    void findById_whenNotFound_shouldReturn404() {


        when(service.findById("m1"))
                .thenReturn(Mono.empty());


        webTestClient.get()
                .uri("/movements/m1")
                .exchange()
                .expectStatus()
                .isNotFound();
    }


    @Test
    void findByAccount_shouldReturnMovements() {


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


        when(service.findByAccount("a1"))
                .thenReturn(
                        Flux.just(movement1, movement2)
                );


        webTestClient.get()
                .uri("/movements/account/a1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Movement.class)
                .hasSize(2);
    }


    @Test
    void getLast10Movements_shouldReturnMovements() {


        Movement movement = Movement.builder()
                .id("m1")
                .accountId("a1")
                .amount(BigDecimal.valueOf(100.0))
                .build();


        when(service.getLast10Movements("a1"))
                .thenReturn(Flux.just(movement));


        webTestClient.get()
                .uri("/movements/account/a1/last10")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Movement.class)
                .hasSize(1);
    }



    @Test
    void getMovementsByDateRange_shouldReturnMovements() {


        LocalDateTime start =
                LocalDateTime.of(2026,1,1,0,0);

        LocalDateTime end =
                LocalDateTime.of(2026,1,31,23,59);


        Movement movement = Movement.builder()
                .id("m1")
                .accountId("a1")
                .amount(BigDecimal.valueOf(100.0))
                .build();


        when(service.findByAccountAndDateRange(
                eq("a1"),
                eq(start),
                eq(end)
        ))
                .thenReturn(Flux.just(movement));


        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movements/account/a1/report")
                        .queryParam("startDate", start)
                        .queryParam("endDate", end)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Movement.class)
                .hasSize(1);
    }

}

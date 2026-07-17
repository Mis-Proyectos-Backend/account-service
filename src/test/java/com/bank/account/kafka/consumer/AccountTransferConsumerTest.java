package com.bank.account.kafka.consumer;

import com.bank.account.dto.TransferRequest;
import com.bank.account.enums.PaymentMethod;
import com.bank.account.event.AccountTransferEvent;
import com.bank.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountTransferConsumerTest {


    @Mock
    private AccountService accountService;


    private AccountTransferConsumer consumer;


    @BeforeEach
    void setUp() {

        consumer = new AccountTransferConsumer(accountService);

    }


    @Test
    void consume_shouldProcessTransferSuccessfully() {


        AccountTransferEvent event =
                AccountTransferEvent.builder()
                        .sourceAccountId("account001")
                        .destinationAccountId("account002")
                        .amount(BigDecimal.valueOf(100))
                        .paymentMethod(PaymentMethod.YANKI)
                        .description("Yanki transfer")
                        .date(LocalDateTime.now())
                        .build();


        when(accountService.transfer(any(TransferRequest.class)))
                .thenReturn(Mono.empty());


        consumer.consume(event);


        ArgumentCaptor<TransferRequest> captor =
                ArgumentCaptor.forClass(TransferRequest.class);


        verify(accountService)
                .transfer(captor.capture());


        TransferRequest request =
                captor.getValue();


        assertEquals(
                "account001",
                request.getSourceAccountId()
        );


        assertEquals(
                "account002",
                request.getDestinationAccountId()
        );


        assertEquals(
                BigDecimal.valueOf(100),
                request.getAmount()
        );


        assertEquals(
                PaymentMethod.YANKI,
                request.getPaymentMethod()
        );

    }



    @Test
    void consume_shouldHandleTransferError() {


        AccountTransferEvent event =
                AccountTransferEvent.builder()
                        .sourceAccountId("account001")
                        .destinationAccountId("account002")
                        .amount(BigDecimal.valueOf(100))
                        .paymentMethod(PaymentMethod.YANKI)
                        .build();


        when(accountService.transfer(any(TransferRequest.class)))
                .thenReturn(
                        Mono.error(
                                new RuntimeException(
                                        "Insufficient balance"
                                )
                        )
                );


        assertDoesNotThrow(() ->
                consumer.consume(event)
        );


        verify(accountService)
                .transfer(any(TransferRequest.class));

    }

}

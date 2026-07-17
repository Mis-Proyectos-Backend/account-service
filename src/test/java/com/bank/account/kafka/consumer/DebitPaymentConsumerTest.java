package com.bank.account.kafka.consumer;

import com.bank.account.client.dto.WithdrawRequest;
import com.bank.account.enums.PaymentMethod;
import com.bank.account.event.DebitPaymentEvent;
import com.bank.account.model.Account;
import com.bank.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DebitPaymentConsumerTest {

    private AccountService accountService;

    private DebitPaymentConsumer consumer;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        consumer = new DebitPaymentConsumer(accountService);
    }

    @Test
    void consume_shouldWithdrawFromAccount() {

        DebitPaymentEvent event =
                DebitPaymentEvent.builder()
                        .accountId("a1")
                        .amount(BigDecimal.valueOf(100))
                        .build();

        Account account =
                Account.builder()
                        .id("a1")
                        .build();

        when(accountService.withdraw(eq("a1"), any(WithdrawRequest.class)))
                .thenReturn(Mono.just(account));

        consumer.consume(event);

        ArgumentCaptor<WithdrawRequest> captor =
                ArgumentCaptor.forClass(WithdrawRequest.class);

        verify(accountService)
                .withdraw(eq("a1"), captor.capture());

        WithdrawRequest request = captor.getValue();

        assertEquals(BigDecimal.valueOf(100), request.getAmount());
        assertEquals(PaymentMethod.DEBIT_CARD, request.getPaymentMethod());
    }

    @Test
    void consume_whenWithdrawFails_shouldNotThrowException() {

        DebitPaymentEvent event =
                DebitPaymentEvent.builder()
                        .accountId("a1")
                        .amount(BigDecimal.valueOf(100))
                        .build();

        when(accountService.withdraw(eq("a1"), any(WithdrawRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Insufficient funds")));

        consumer.consume(event);

        verify(accountService)
                .withdraw(eq("a1"), any(WithdrawRequest.class));
    }
}

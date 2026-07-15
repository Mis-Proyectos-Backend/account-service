package com.bank.account.kafka.consumer;

import com.bank.account.client.dto.WithdrawRequest;
import com.bank.account.enums.PaymentMethod;
import com.bank.account.event.DebitPaymentEvent;
import com.bank.account.service.AccountService;

import lombok.RequiredArgsConstructor;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class DebitPaymentConsumer {


    private final AccountService accountService;


    @KafkaListener(
            topics = "debit-payment-topic",
            groupId = "account-group",
            containerFactory = "debitPaymentKafkaListenerContainerFactory"
    )
    public void consume(DebitPaymentEvent event) {

        WithdrawRequest request = WithdrawRequest.builder()
                .amount(event.getAmount())
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .build();


        accountService.withdraw(
                        event.getAccountId(),
                        request
                )
                .doOnSuccess(account ->
                        System.out.println(
                                "Debit payment processed: "
                                        + account.getId() + " " + request.getPaymentMethod()
                        )
                )
                .doOnError(error ->
                        System.err.println(
                                "Debit payment failed: "
                                        + error.getMessage()
                        )
                )
                .subscribe();

    }
}

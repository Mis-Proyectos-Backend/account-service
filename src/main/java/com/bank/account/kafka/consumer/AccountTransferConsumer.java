package com.bank.account.kafka.consumer;

import com.bank.account.dto.TransferRequest;
import com.bank.account.event.AccountTransferEvent;
import com.bank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountTransferConsumer {


    private final AccountService accountService;


    @KafkaListener(
            topics = "account-transfer-topic",
            groupId = "account-group"
    )
    public void consume(AccountTransferEvent event) {


        TransferRequest request = TransferRequest.builder()
                .sourceAccountId(event.getSourceAccountId())
                .destinationAccountId(event.getDestinationAccountId())
                .amount(event.getAmount())
                .paymentMethod(event.getPaymentMethod())
                .build();

        accountService.transfer(request)
                .doOnSuccess(result ->
                        System.out.println(
                                "Yanki transfer processed "
                                        + event.getAmount()
                        )
                )
                .doOnError(error ->
                        System.err.println(
                                "Yanki transfer failed "
                                        + error.getMessage()
                        )
                )
                .subscribe();

    }

}

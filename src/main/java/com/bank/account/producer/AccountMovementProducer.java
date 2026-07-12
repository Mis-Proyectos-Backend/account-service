package com.bank.account.producer;

import com.bank.account.event.AccountMovementEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AccountMovementProducer {

    private static final String TOPIC = "account-movements";

    private final KafkaTemplate<String, AccountMovementEvent> kafkaTemplate;

    public Mono<Void> send(AccountMovementEvent event) {

        return Mono.fromRunnable(() ->
                kafkaTemplate.send(TOPIC, event.getAccountId(), event));

    }

}
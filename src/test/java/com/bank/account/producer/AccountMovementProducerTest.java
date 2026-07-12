package com.bank.account.producer;

import com.bank.account.enums.AccountType;
import com.bank.account.enums.MovementType;
import com.bank.account.event.AccountMovementEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountMovementProducerTest {

    @Mock
    private KafkaTemplate<String, AccountMovementEvent> kafkaTemplate;

    @InjectMocks
    private AccountMovementProducer producer;

    @Test
    void send_shouldPublishEventToKafka() {

        AccountMovementEvent event = AccountMovementEvent.builder()
                .accountId("a1")
                .accountNumber("ACC001")
                .accountType(AccountType.CHECKING)
                .customerId("c1")
                .movementType(MovementType.DEPOSIT)
                .amount(BigDecimal.valueOf(500))
                .balanceAfterMovement(BigDecimal.valueOf(1500))
                .movementDate(LocalDateTime.now())
                .build();

        StepVerifier.create(producer.send(event))
                .verifyComplete();

        verify(kafkaTemplate)
                .send("account-movements", "a1", event);
    }
}
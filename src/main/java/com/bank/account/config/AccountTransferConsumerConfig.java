package com.bank.account.config;

import com.bank.account.event.AccountTransferEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AccountTransferConsumerConfig {


    @Bean
    public ConsumerFactory<String, AccountTransferEvent> accountTransferConsumerFactory() {


        JsonDeserializer<AccountTransferEvent> deserializer =
                new JsonDeserializer<>(AccountTransferEvent.class);

        deserializer.addTrustedPackages("*");

        deserializer.setRemoveTypeHeaders(false);


        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "account-group"
        );

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );


        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }


    @Bean(name = "accountTransferKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, AccountTransferEvent>
    accountTransferKafkaListenerContainerFactory() {


        ConcurrentKafkaListenerContainerFactory<String, AccountTransferEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(accountTransferConsumerFactory());

        return factory;
    }
}
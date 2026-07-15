package com.bank.account.config;

import com.bank.account.event.DebitPaymentEvent;
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
public class KafkaConsumerConfig {


    @Bean
    public ConsumerFactory<String, DebitPaymentEvent> consumerFactory() {

        JsonDeserializer<DebitPaymentEvent> deserializer =
                new JsonDeserializer<>(DebitPaymentEvent.class, false);

        deserializer.addTrustedPackages("*");


        Map<String, Object> props = new HashMap<>();

        props.put(
                org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        props.put(
                org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG,
                "movement-group"
        );


        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }


    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DebitPaymentEvent>
    kafkaListenerContainerFactory() {


        ConcurrentKafkaListenerContainerFactory<String, DebitPaymentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        return factory;
    }
}

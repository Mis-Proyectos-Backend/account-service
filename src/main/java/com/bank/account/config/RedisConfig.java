package com.bank.account.config;

import com.bank.account.model.Account;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisConfig {


    @Bean
    public ReactiveRedisTemplate<String, Account> accountRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        Jackson2JsonRedisSerializer<Account> serializer =
                new Jackson2JsonRedisSerializer<>(Account.class);

        RedisSerializationContext<String, Account> context =
                RedisSerializationContext
                        .<String, Account>newSerializationContext(
                                RedisSerializer.string()
                        )
                        .value(serializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
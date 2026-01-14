package com.mendel.challenge.infrastructure.config;

import com.mendel.challenge.domain.port.out.TransactionRepository;
import com.mendel.challenge.infrastructure.adapter.memory.InMemoryTransactionRepository;
import com.mendel.challenge.infrastructure.adapter.redis.RedisTransactionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RepositoryConfig {

    @Bean
    @ConditionalOnProperty(name = "storage.strategy", havingValue = "memory", matchIfMissing = true)
    public TransactionRepository transactionRepository() { // Nombre del método será el nombre del Bean
        return new InMemoryTransactionRepository();
    }

    @Bean
    @ConditionalOnProperty(name = "storage.strategy", havingValue = "redis")
    public TransactionRepository redisTransactionRepository(RedisTemplate<String, String> redisTemplate) {
        return new RedisTransactionRepository(redisTemplate);
    }
}

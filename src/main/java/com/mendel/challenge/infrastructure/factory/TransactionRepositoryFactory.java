package com.mendel.challenge.infrastructure.factory;

import com.mendel.challenge.domain.model.StorageStrategy;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class TransactionRepositoryFactory {

    private final TransactionRepository inMemoryRepository;
    private final TransactionRepository redisRepository;

    public TransactionRepositoryFactory(
            @Qualifier("inMemoryRepository") TransactionRepository inMemoryRepository,
            @Qualifier("redisRepository") TransactionRepository redisRepository) {
        this.inMemoryRepository = inMemoryRepository;
        this.redisRepository = redisRepository;
    }

    public TransactionRepository getRepository(StorageStrategy strategy) {
        return switch (strategy) {
            case IN_MEMORY -> inMemoryRepository;
            case REDIS -> redisRepository;
        };
    }
}

package com.mendel.challenge.infrastructure.factory;

import com.mendel.challenge.domain.model.StorageStrategy;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TransactionRepositoryFactory {

    private static final Logger log = LoggerFactory.getLogger(TransactionRepositoryFactory.class);

    private final TransactionRepository inMemoryRepository;
    private final TransactionRepository redisRepository;
    private final boolean redisAvailable;

    public TransactionRepositoryFactory(
            @Qualifier("inMemoryRepository") TransactionRepository inMemoryRepository,
            @Qualifier("redisRepository") Optional<TransactionRepository> redisRepository) {

        this.inMemoryRepository = inMemoryRepository;
        this.redisRepository = redisRepository.orElse(null);
        this.redisAvailable = redisRepository.isPresent();

        log.info("TransactionRepositoryFactory initialized - Redis available: {}", redisAvailable);
    }

    public TransactionRepository getRepository(StorageStrategy strategy) {
        if (strategy == StorageStrategy.REDIS) {
            if (!redisAvailable) {
                log.warn("Redis requested but not available, falling back to IN_MEMORY");
                return inMemoryRepository;
            }
            return redisRepository;
        }
        return inMemoryRepository;
    }

    public boolean isRedisAvailable() {
        return redisAvailable;
    }
}

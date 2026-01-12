package com.mendel.challenge.infrastructure.factory;

import com.mendel.challenge.domain.model.StorageStrategy;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionRepositoryFactory Unit Tests")
class TransactionRepositoryFactoryTest {

    @Mock
    private TransactionRepository inMemoryRepository;

    @Mock
    private TransactionRepository redisRepository;

    @BeforeEach
    void setUp() {
        // Usar lenient() para permitir que algunos tests no usen todos los mocks
        lenient().when(inMemoryRepository.getImplementationType()).thenReturn("IN_MEMORY");
        lenient().when(redisRepository.getImplementationType()).thenReturn("REDIS");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize factory with both repositories when Redis is available")
        void shouldInitializeFactoryWithBothRepositoriesWhenRedisAvailable() {
            // When
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            );

            // Then
            assertThat(factory).isNotNull();
            assertThat(factory.isRedisAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should initialize factory with only in-memory when Redis is not available")
        void shouldInitializeFactoryWithOnlyInMemoryWhenRedisNotAvailable() {
            // When
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.empty()
            );

            // Then
            assertThat(factory).isNotNull();
            assertThat(factory.isRedisAvailable()).isFalse();
        }

        @Test
        @DisplayName("Should set redisAvailable to true when Redis repository is present")
        void shouldSetRedisAvailableToTrueWhenRedisRepositoryPresent() {
            // When
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            );

            // Then
            assertThat(factory.isRedisAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should set redisAvailable to false when Redis repository is absent")
        void shouldSetRedisAvailableToFalseWhenRedisRepositoryAbsent() {
            // When
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.empty()
            );

            // Then
            assertThat(factory.isRedisAvailable()).isFalse();
        }

        @Test
        @DisplayName("Should not throw exception when both repositories are provided")
        void shouldNotThrowExceptionWhenBothRepositoriesProvided() {
            // When & Then
            assertThatCode(() -> new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should not throw exception when only in-memory repository is provided")
        void shouldNotThrowExceptionWhenOnlyInMemoryRepositoryProvided() {
            // When & Then
            assertThatCode(() -> new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.empty()
            )).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Get Repository Tests - With Redis Available")
    class GetRepositoryWithRedisTests {

        private TransactionRepositoryFactory factory;

        @BeforeEach
        void setUpNested() {
            factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            );
        }

        @Test
        @DisplayName("Should return in-memory repository when IN_MEMORY strategy is requested")
        void shouldReturnInMemoryRepositoryWhenInMemoryStrategyRequested() {
            // When
            TransactionRepository result = factory.getRepository(StorageStrategy.IN_MEMORY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isSameAs(inMemoryRepository);
            assertThat(result.getImplementationType()).isEqualTo("IN_MEMORY");
        }

        @Test
        @DisplayName("Should return Redis repository when REDIS strategy is requested")
        void shouldReturnRedisRepositoryWhenRedisStrategyRequested() {
            // When
            TransactionRepository result = factory.getRepository(StorageStrategy.REDIS);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isSameAs(redisRepository);
            assertThat(result.getImplementationType()).isEqualTo("REDIS");
        }

        @Test
        @DisplayName("Should return correct repository for multiple calls")
        void shouldReturnCorrectRepositoryForMultipleCalls() {
            // When
            TransactionRepository result1 = factory.getRepository(StorageStrategy.IN_MEMORY);
            TransactionRepository result2 = factory.getRepository(StorageStrategy.REDIS);
            TransactionRepository result3 = factory.getRepository(StorageStrategy.IN_MEMORY);

            // Then
            assertThat(result1).isSameAs(inMemoryRepository);
            assertThat(result2).isSameAs(redisRepository);
            assertThat(result3).isSameAs(inMemoryRepository);
        }

        @Test
        @DisplayName("Should always return same instance for same strategy")
        void shouldAlwaysReturnSameInstanceForSameStrategy() {
            // When
            TransactionRepository result1 = factory.getRepository(StorageStrategy.REDIS);
            TransactionRepository result2 = factory.getRepository(StorageStrategy.REDIS);

            // Then
            assertThat(result1).isSameAs(result2);
        }
    }

    @Nested
    @DisplayName("Get Repository Tests - Without Redis Available")
    class GetRepositoryWithoutRedisTests {

        private TransactionRepositoryFactory factory;

        @BeforeEach
        void setUpNested() {
            factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.empty()
            );
        }

        @Test
        @DisplayName("Should return in-memory repository when IN_MEMORY strategy is requested")
        void shouldReturnInMemoryRepositoryWhenInMemoryStrategyRequested() {
            // When
            TransactionRepository result = factory.getRepository(StorageStrategy.IN_MEMORY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isSameAs(inMemoryRepository);
            assertThat(result.getImplementationType()).isEqualTo("IN_MEMORY");
        }

        @Test
        @DisplayName("Should fallback to in-memory when REDIS strategy is requested but Redis not available")
        void shouldFallbackToInMemoryWhenRedisStrategyRequestedButRedisNotAvailable() {
            // When
            TransactionRepository result = factory.getRepository(StorageStrategy.REDIS);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isSameAs(inMemoryRepository);
            assertThat(result.getImplementationType()).isEqualTo("IN_MEMORY");
        }

        @Test
        @DisplayName("Should always return in-memory repository regardless of strategy")
        void shouldAlwaysReturnInMemoryRepositoryRegardlessOfStrategy() {
            // When
            TransactionRepository result1 = factory.getRepository(StorageStrategy.IN_MEMORY);
            TransactionRepository result2 = factory.getRepository(StorageStrategy.REDIS);

            // Then
            assertThat(result1).isSameAs(inMemoryRepository);
            assertThat(result2).isSameAs(inMemoryRepository);
            assertThat(result1).isSameAs(result2);
        }

        @Test
        @DisplayName("Should not throw exception when Redis is requested but not available")
        void shouldNotThrowExceptionWhenRedisRequestedButNotAvailable() {
            // When & Then
            assertThatCode(() -> factory.getRepository(StorageStrategy.REDIS))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Is Redis Available Tests")
    class IsRedisAvailableTests {

        @Test
        @DisplayName("Should return true when Redis repository is present")
        void shouldReturnTrueWhenRedisRepositoryPresent() {
            // Given
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            );

            // When
            boolean result = factory.isRedisAvailable();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when Redis repository is absent")
        void shouldReturnFalseWhenRedisRepositoryAbsent() {
            // Given
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.empty()
            );

            // When
            boolean result = factory.isRedisAvailable();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should consistently return same value across multiple calls")
        void shouldConsistentlyReturnSameValueAcrossMultipleCalls() {
            // Given
            TransactionRepositoryFactory factoryWithRedis = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            );

            TransactionRepositoryFactory factoryWithoutRedis = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.empty()
            );

            // When & Then
            assertThat(factoryWithRedis.isRedisAvailable()).isTrue();
            assertThat(factoryWithRedis.isRedisAvailable()).isTrue();
            assertThat(factoryWithRedis.isRedisAvailable()).isTrue();

            assertThat(factoryWithoutRedis.isRedisAvailable()).isFalse();
            assertThat(factoryWithoutRedis.isRedisAvailable()).isFalse();
            assertThat(factoryWithoutRedis.isRedisAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Strategy Switch Tests")
    class StrategySwitchTests {

        @Test
        @DisplayName("Should handle alternating strategy requests correctly")
        void shouldHandleAlternatingStrategyRequestsCorrectly() {
            // Given
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            );

            // When
            TransactionRepository r1 = factory.getRepository(StorageStrategy.IN_MEMORY);
            TransactionRepository r2 = factory.getRepository(StorageStrategy.REDIS);
            TransactionRepository r3 = factory.getRepository(StorageStrategy.IN_MEMORY);
            TransactionRepository r4 = factory.getRepository(StorageStrategy.REDIS);

            // Then
            assertThat(r1).isSameAs(inMemoryRepository);
            assertThat(r2).isSameAs(redisRepository);
            assertThat(r3).isSameAs(inMemoryRepository);
            assertThat(r4).isSameAs(redisRepository);
        }

        @Test
        @DisplayName("Should handle rapid strategy switching without issues")
        void shouldHandleRapidStrategySwitchingWithoutIssues() {
            // Given
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            );

            // When & Then - Multiple rapid switches
            for (int i = 0; i < 100; i++) {
                StorageStrategy strategy = (i % 2 == 0) ? StorageStrategy.IN_MEMORY : StorageStrategy.REDIS;
                TransactionRepository repo = factory.getRepository(strategy);

                if (strategy == StorageStrategy.IN_MEMORY) {
                    assertThat(repo).isSameAs(inMemoryRepository);
                } else {
                    assertThat(repo).isSameAs(redisRepository);
                }
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null strategy gracefully by returning in-memory")
        void shouldHandleNullStrategyGracefullyByReturningInMemory() {
            // Given
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            );

            // When
            TransactionRepository result = factory.getRepository(null);

            // Then
            assertThat(result).isSameAs(inMemoryRepository);
        }

        @Test
        @DisplayName("Should work correctly when both repositories are the same instance")
        void shouldWorkCorrectlyWhenBothRepositoriesAreSameInstance() {
            // Given
            TransactionRepository singleRepo = mock(TransactionRepository.class);
            lenient().when(singleRepo.getImplementationType()).thenReturn("SINGLE");

            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    singleRepo,
                    Optional.of(singleRepo)
            );

            // When
            TransactionRepository r1 = factory.getRepository(StorageStrategy.IN_MEMORY);
            TransactionRepository r2 = factory.getRepository(StorageStrategy.REDIS);

            // Then
            assertThat(r1).isSameAs(singleRepo);
            assertThat(r2).isSameAs(singleRepo);
            assertThat(r1).isSameAs(r2);
            assertThat(factory.isRedisAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Integration Scenarios Tests")
    class IntegrationScenariosTests {

        @Test
        @DisplayName("Should support typical application workflow")
        void shouldSupportTypicalApplicationWorkflow() {
            // Given
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            );

            // Simulate typical usage pattern
            // 1. Check if Redis is available
            boolean hasRedis = factory.isRedisAvailable();
            assertThat(hasRedis).isTrue();

            // 2. Get repository based on configuration
            TransactionRepository repo = factory.getRepository(
                    hasRedis ? StorageStrategy.REDIS : StorageStrategy.IN_MEMORY
            );
            assertThat(repo).isSameAs(redisRepository);

            // 3. Fallback scenario
            factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.empty()
            );

            hasRedis = factory.isRedisAvailable();
            assertThat(hasRedis).isFalse();

            repo = factory.getRepository(
                    hasRedis ? StorageStrategy.REDIS : StorageStrategy.IN_MEMORY
            );
            assertThat(repo).isSameAs(inMemoryRepository);
        }

        @Test
        @DisplayName("Should support graceful degradation pattern")
        void shouldSupportGracefulDegradationPattern() {
            // Given - Factory without Redis
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.empty()
            );

            // When - Application tries to use Redis but falls back
            TransactionRepository preferredRepo = factory.getRepository(StorageStrategy.REDIS);
            TransactionRepository fallbackRepo = factory.getRepository(StorageStrategy.IN_MEMORY);

            // Then - Both should be in-memory
            assertThat(preferredRepo).isSameAs(inMemoryRepository);
            assertThat(fallbackRepo).isSameAs(inMemoryRepository);
            assertThat(preferredRepo).isSameAs(fallbackRepo);
        }

        @Test
        @DisplayName("Should maintain consistent behavior in multi-threaded scenario")
        void shouldMaintainConsistentBehaviorInMultiThreadedScenario() throws InterruptedException {
            // Given
            TransactionRepositoryFactory factory = new TransactionRepositoryFactory(
                    inMemoryRepository,
                    Optional.of(redisRepository)
            );

            // When - Multiple threads access factory simultaneously
            Thread[] threads = new Thread[10];
            for (int i = 0; i < 10; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    StorageStrategy strategy = (index % 2 == 0)
                            ? StorageStrategy.IN_MEMORY
                            : StorageStrategy.REDIS;

                    TransactionRepository repo = factory.getRepository(strategy);

                    if (strategy == StorageStrategy.IN_MEMORY) {
                        assertThat(repo).isSameAs(inMemoryRepository);
                    } else {
                        assertThat(repo).isSameAs(redisRepository);
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Then - Factory state should remain consistent
            assertThat(factory.isRedisAvailable()).isTrue();
        }
    }
}

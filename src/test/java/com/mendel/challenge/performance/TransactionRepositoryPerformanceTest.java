package com.mendel.challenge.performance;

import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.infrastructure.adapter.memory.InMemoryTransactionRepository;
import com.mendel.challenge.infrastructure.adapter.redis.RedisTransactionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Transaction Repository Performance Tests")
class TransactionRepositoryPerformanceTest {

    @Autowired
    private InMemoryTransactionRepository inMemoryRepository;

    @Autowired
    private RedisTransactionRepository redisRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final int WARMUP_ITERATIONS = 100;
    private static final int TEST_ITERATIONS = 1000;
    private static final int CONCURRENT_THREADS = 10;

    @BeforeEach
    void setUp() {
        // Limpiar Redis
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Nested
    @DisplayName("Single Thread Performance Tests")
    class SingleThreadPerformanceTests {

        @Test
        @Order(1)
        @DisplayName("Compare save performance - IN_MEMORY vs REDIS")
        void compareSavePerformance() {
            System.out.println("\n=== SAVE PERFORMANCE TEST ===");

            // Warmup
            warmupSave(inMemoryRepository, WARMUP_ITERATIONS);
            warmupSave(redisRepository, WARMUP_ITERATIONS);

            // Test IN_MEMORY
            long inMemoryTime = measureSavePerformance(inMemoryRepository, TEST_ITERATIONS);
            System.out.printf("IN_MEMORY Save: %d transactions in %d ms (%.2f tx/sec)%n",
                    TEST_ITERATIONS, inMemoryTime, (TEST_ITERATIONS * 1000.0) / inMemoryTime);

            // Test REDIS
            long redisTime = measureSavePerformance(redisRepository, TEST_ITERATIONS);
            System.out.printf("REDIS Save: %d transactions in %d ms (%.2f tx/sec)%n",
                    TEST_ITERATIONS, redisTime, (TEST_ITERATIONS * 1000.0) / redisTime);

            // Comparison
            double ratio = (double) redisTime / inMemoryTime;
            System.out.printf("Redis is %.2fx slower than IN_MEMORY for saves%n", ratio);

            assertThat(inMemoryTime).isLessThan(redisTime);
        }

        @Test
        @Order(2)
        @DisplayName("Compare findById performance - IN_MEMORY vs REDIS")
        void compareFindByIdPerformance() {
            System.out.println("\n=== FIND BY ID PERFORMANCE TEST ===");

            // Setup - Create test data
            setupTestData(inMemoryRepository, TEST_ITERATIONS);
            setupTestData(redisRepository, TEST_ITERATIONS);

            // Test IN_MEMORY
            long inMemoryTime = measureFindByIdPerformance(inMemoryRepository, TEST_ITERATIONS);
            System.out.printf("IN_MEMORY FindById: %d lookups in %d ms (%.2f lookups/sec)%n",
                    TEST_ITERATIONS, inMemoryTime, (TEST_ITERATIONS * 1000.0) / inMemoryTime);

            // Test REDIS
            long redisTime = measureFindByIdPerformance(redisRepository, TEST_ITERATIONS);
            System.out.printf("REDIS FindById: %d lookups in %d ms (%.2f lookups/sec)%n",
                    TEST_ITERATIONS, redisTime, (TEST_ITERATIONS * 1000.0) / redisTime);

            // Comparison
            double ratio = (double) redisTime / inMemoryTime;
            System.out.printf("Redis is %.2fx slower than IN_MEMORY for findById%n", ratio);
        }

        @Test
        @Order(3)
        @DisplayName("Compare findByType performance - IN_MEMORY vs REDIS")
        void compareFindByTypePerformance() {
            System.out.println("\n=== FIND BY TYPE PERFORMANCE TEST ===");

            // Setup - Create test data with different types
            setupTestDataWithTypes(inMemoryRepository, TEST_ITERATIONS);
            setupTestDataWithTypes(redisRepository, TEST_ITERATIONS);

            // Test IN_MEMORY
            long inMemoryTime = measureFindByTypePerformance(inMemoryRepository, TEST_ITERATIONS);
            System.out.printf("IN_MEMORY FindByType: %d queries in %d ms%n",
                    TEST_ITERATIONS / 10, inMemoryTime);

            // Test REDIS
            long redisTime = measureFindByTypePerformance(redisRepository, TEST_ITERATIONS);
            System.out.printf("REDIS FindByType: %d queries in %d ms%n",
                    TEST_ITERATIONS / 10, redisTime);
        }

        @Test
        @Order(4)
        @DisplayName("Compare findChildrenOf performance - IN_MEMORY vs REDIS")
        void compareFindChildrenOfPerformance() {
            System.out.println("\n=== FIND CHILDREN PERFORMANCE TEST ===");

            // Setup - Create hierarchies
            setupHierarchyTestData(inMemoryRepository, 100);
            setupHierarchyTestData(redisRepository, 100);

            // Test IN_MEMORY
            long inMemoryTime = measureFindChildrenPerformance(inMemoryRepository, 100);
            System.out.printf("IN_MEMORY FindChildren: queries in %d ms%n", inMemoryTime);

            // Test REDIS
            long redisTime = measureFindChildrenPerformance(redisRepository, 100);
            System.out.printf("REDIS FindChildren: queries in %d ms%n", redisTime);
        }
    }

    @Nested
    @DisplayName("Concurrent Performance Tests")
    class ConcurrentPerformanceTests {

        @Test
        @DisplayName("Stress test - Concurrent writes IN_MEMORY")
        void stressTestConcurrentWritesInMemory() throws InterruptedException, ExecutionException {
            System.out.println("\n=== CONCURRENT WRITES STRESS TEST - IN_MEMORY ===");

            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            int operationsPerThread = TEST_ITERATIONS / CONCURRENT_THREADS;

            Instant start = Instant.now();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                final int threadId = i;
                Future<?> future = executor.submit(() -> {
                    for (int j = 0; j < operationsPerThread; j++) {
                        long id = threadId * operationsPerThread + j;
                        Transaction tx = createTransaction(id, "type" + (j % 10));
                        inMemoryRepository.save(tx);
                    }
                });
                futures.add(future);
            }

            // Wait for all threads to complete
            for (Future<?> future : futures) {
                future.get();
            }

            Duration duration = Duration.between(start, Instant.now());
            executor.shutdown();

            System.out.printf("Completed %d concurrent writes in %d ms (%.2f tx/sec)%n",
                    TEST_ITERATIONS, duration.toMillis(),
                    (TEST_ITERATIONS * 1000.0) / duration.toMillis());
        }

        @Test
        @DisplayName("Stress test - Concurrent writes REDIS")
        void stressTestConcurrentWritesRedis() throws InterruptedException, ExecutionException {
            System.out.println("\n=== CONCURRENT WRITES STRESS TEST - REDIS ===");

            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            int operationsPerThread = TEST_ITERATIONS / CONCURRENT_THREADS;

            Instant start = Instant.now();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                final int threadId = i;
                Future<?> future = executor.submit(() -> {
                    for (int j = 0; j < operationsPerThread; j++) {
                        long id = 10000 + threadId * operationsPerThread + j;
                        Transaction tx = createTransaction(id, "type" + (j % 10));
                        redisRepository.save(tx);
                    }
                });
                futures.add(future);
            }

            for (Future<?> future : futures) {
                future.get();
            }

            Duration duration = Duration.between(start, Instant.now());
            executor.shutdown();

            System.out.printf("Completed %d concurrent writes in %d ms (%.2f tx/sec)%n",
                    TEST_ITERATIONS, duration.toMillis(),
                    (TEST_ITERATIONS * 1000.0) / duration.toMillis());
        }

        @Test
        @DisplayName("Stress test - Mixed operations IN_MEMORY")
        void stressTestMixedOperationsInMemory() throws InterruptedException, ExecutionException {
            System.out.println("\n=== MIXED OPERATIONS STRESS TEST - IN_MEMORY ===");

            // Setup initial data
            setupTestData(inMemoryRepository, 1000);

            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

            Instant start = Instant.now();

            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        Random random = new Random();
                        for (int j = 0; j < 100; j++) {
                            int operation = random.nextInt(3);
                            switch (operation) {
                                case 0: // Save
                                    long id = 10000 + threadId * 1000 + j;
                                    inMemoryRepository.save(createTransaction(id, "type" + j));
                                    break;
                                case 1: // FindById
                                    inMemoryRepository.findById((long) random.nextInt(1000));
                                    break;
                                case 2: // FindByType
                                    inMemoryRepository.findByType("type" + random.nextInt(10));
                                    break;
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            Duration duration = Duration.between(start, Instant.now());
            executor.shutdown();

            System.out.printf("Completed mixed operations in %d ms%n", duration.toMillis());
        }

        @Test
        @DisplayName("Stress test - Read heavy workload REDIS")
        void stressTestReadHeavyWorkloadRedis() throws InterruptedException, ExecutionException {
            System.out.println("\n=== READ HEAVY WORKLOAD - REDIS ===");

            // Setup data
            setupTestData(redisRepository, 1000);

            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            int readsPerThread = 500;

            Instant start = Instant.now();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                Future<?> future = executor.submit(() -> {
                    Random random = new Random();
                    for (int j = 0; j < readsPerThread; j++) {
                        redisRepository.findById((long) random.nextInt(1000));
                    }
                });
                futures.add(future);
            }

            for (Future<?> future : futures) {
                future.get();
            }

            Duration duration = Duration.between(start, Instant.now());
            executor.shutdown();

            int totalReads = CONCURRENT_THREADS * readsPerThread;
            System.out.printf("Completed %d concurrent reads in %d ms (%.2f reads/sec)%n",
                    totalReads, duration.toMillis(),
                    (totalReads * 1000.0) / duration.toMillis());
        }
    }

    @Nested
    @DisplayName("Memory and Resource Tests")
    class MemoryAndResourceTests {

        @Test
        @DisplayName("Memory usage test - IN_MEMORY with large dataset")
        void memoryUsageTestInMemory() {
            System.out.println("\n=== MEMORY USAGE TEST - IN_MEMORY ===");

            Runtime runtime = Runtime.getRuntime();
            runtime.gc();

            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("Memory before: %.2f MB%n", memoryBefore / (1024.0 * 1024.0));

            // Create large dataset
            int largeDatasetSize = 10000;
            for (int i = 0; i < largeDatasetSize; i++) {
                inMemoryRepository.save(createTransaction(i, "type" + (i % 100)));
            }

            runtime.gc();
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            System.out.printf("Memory after: %.2f MB%n", memoryAfter / (1024.0 * 1024.0));
            System.out.printf("Memory used: %.2f MB for %d transactions%n",
                    (memoryAfter - memoryBefore) / (1024.0 * 1024.0), largeDatasetSize);
            System.out.printf("Average per transaction: %.2f KB%n",
                    (memoryAfter - memoryBefore) / (double) largeDatasetSize / 1024.0);
        }

        @Test
        @DisplayName("Scalability test - Performance degradation with dataset size")
        void scalabilityTest() {
            System.out.println("\n=== SCALABILITY TEST ===");

            int[] datasetSizes = {100, 500, 1000, 5000, 10000};

            System.out.println("Dataset Size | IN_MEMORY (ms) | REDIS (ms) | Ratio");
            System.out.println("-------------|----------------|------------|-------");

            for (int size : datasetSizes) {
                // Setup data
                setupTestData(inMemoryRepository, size);
                setupTestData(redisRepository, size);

                // Test IN_MEMORY
                long inMemoryTime = measureFindByIdPerformance(inMemoryRepository, size);

                // Test REDIS
                long redisTime = measureFindByIdPerformance(redisRepository, size);

                double ratio = (double) redisTime / inMemoryTime;
                System.out.printf("%12d | %14d | %10d | %.2fx%n",
                        size, inMemoryTime, redisTime, ratio);
            }
        }
    }

    // Helper methods
    private void warmupSave(Object repository, int iterations) {
        for (int i = 0; i < iterations; i++) {
            Transaction tx = createTransaction(i, "warmup");
            if (repository instanceof InMemoryTransactionRepository) {
                ((InMemoryTransactionRepository) repository).save(tx);
            } else {
                ((RedisTransactionRepository) repository).save(tx);
            }
        }
    }

    private long measureSavePerformance(Object repository, int iterations) {
        Instant start = Instant.now();

        for (int i = 0; i < iterations; i++) {
            Transaction tx = createTransaction(i, "type" + (i % 10));
            if (repository instanceof InMemoryTransactionRepository) {
                ((InMemoryTransactionRepository) repository).save(tx);
            } else {
                ((RedisTransactionRepository) repository).save(tx);
            }
        }

        return Duration.between(start, Instant.now()).toMillis();
    }

    private long measureFindByIdPerformance(Object repository, int iterations) {
        Instant start = Instant.now();
        Random random = new Random();

        for (int i = 0; i < iterations; i++) {
            long id = random.nextInt(iterations);
            if (repository instanceof InMemoryTransactionRepository) {
                ((InMemoryTransactionRepository) repository).findById(id);
            } else {
                ((RedisTransactionRepository) repository).findById(id);
            }
        }

        return Duration.between(start, Instant.now()).toMillis();
    }

    private long measureFindByTypePerformance(Object repository, int iterations) {
        Instant start = Instant.now();

        for (int i = 0; i < iterations / 10; i++) {
            String type = "type" + (i % 10);
            if (repository instanceof InMemoryTransactionRepository) {
                ((InMemoryTransactionRepository) repository).findByType(type);
            } else {
                ((RedisTransactionRepository) repository).findByType(type);
            }
        }

        return Duration.between(start, Instant.now()).toMillis();
    }

    private long measureFindChildrenPerformance(Object repository, int parents) {
        Instant start = Instant.now();

        for (int i = 0; i < parents; i++) {
            long parentId = i * 10L;
            if (repository instanceof InMemoryTransactionRepository) {
                ((InMemoryTransactionRepository) repository).findChildrenOf(parentId);
            } else {
                ((RedisTransactionRepository) repository).findChildrenOf(parentId);
            }
        }

        return Duration.between(start, Instant.now()).toMillis();
    }

    private void setupTestData(Object repository, int count) {
        for (int i = 0; i < count; i++) {
            Transaction tx = createTransaction(i, "type" + (i % 10));
            if (repository instanceof InMemoryTransactionRepository) {
                ((InMemoryTransactionRepository) repository).save(tx);
            } else {
                ((RedisTransactionRepository) repository).save(tx);
            }
        }
    }

    private void setupTestDataWithTypes(Object repository, int count) {
        for (int i = 0; i < count; i++) {
            Transaction tx = createTransaction(i, "type" + (i % 10));
            if (repository instanceof InMemoryTransactionRepository) {
                ((InMemoryTransactionRepository) repository).save(tx);
            } else {
                ((RedisTransactionRepository) repository).save(tx);
            }
        }
    }

    private void setupHierarchyTestData(Object repository, int parents) {
        for (int i = 0; i < parents; i++) {
            long parentId = i * 10L;
            Transaction parent = createTransaction(parentId, "parent");

            if (repository instanceof InMemoryTransactionRepository) {
                ((InMemoryTransactionRepository) repository).save(parent);
                // Create 5 children for each parent
                for (int j = 1; j <= 5; j++) {
                    Transaction child = createTransactionWithParent(parentId + j, "child", parentId);
                    ((InMemoryTransactionRepository) repository).save(child);
                }
            } else {
                ((RedisTransactionRepository) repository).save(parent);
                for (int j = 1; j <= 5; j++) {
                    Transaction child = createTransactionWithParent(parentId + j, "child", parentId);
                    ((RedisTransactionRepository) repository).save(child);
                }
            }
        }
    }

    private Transaction createTransaction(long id, String type) {
        return Transaction.builder()
                .id(id)
                .type(type)
                .amount(new BigDecimal("100.00"))
                .build();
    }

    private Transaction createTransactionWithParent(long id, String type, long parentId) {
        return Transaction.builder()
                .id(id)
                .type(type)
                .amount(new BigDecimal("50.00"))
                .parentId(parentId)
                .build();
    }
}

package com.mendel.challenge.performance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("API Load and Stress Tests")
class TransactionApiLoadTest {

    @LocalServerPort
    private int port;

    private static final int WARMUP_REQUESTS = 50;
    private static final int LOAD_TEST_REQUESTS = 500;
    private static final int STRESS_TEST_REQUESTS = 1000;
    private static final int CONCURRENT_USERS = 20;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/transactionservice";
    }

    @Test
    @DisplayName("Load test - Sequential requests")
    void loadTestSequentialRequests() {
        System.out.println("\n=== LOAD TEST - SEQUENTIAL REQUESTS ===");

        // Warmup
        warmup();

        // Test IN_MEMORY
        long inMemoryTime = testSequentialCreates("memory", LOAD_TEST_REQUESTS);
        System.out.printf("IN_MEMORY: %d requests in %d ms (%.2f req/sec)%n",
                LOAD_TEST_REQUESTS, inMemoryTime,
                (LOAD_TEST_REQUESTS * 1000.0) / inMemoryTime);

        // Test REDIS
        long redisTime = testSequentialCreates("redis", LOAD_TEST_REQUESTS);
        System.out.printf("REDIS: %d requests in %d ms (%.2f req/sec)%n",
                LOAD_TEST_REQUESTS, redisTime,
                (LOAD_TEST_REQUESTS * 1000.0) / redisTime);
    }

    @Test
    @DisplayName("Stress test - Concurrent users creating transactions")
    void stressTestConcurrentCreates() throws InterruptedException, ExecutionException {
        System.out.println("\n=== STRESS TEST - CONCURRENT CREATES ===");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        int requestsPerUser = STRESS_TEST_REQUESTS / CONCURRENT_USERS;

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        Instant start = Instant.now();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            Future<?> future = executor.submit(() -> {
                for (int j = 0; j < requestsPerUser; j++) {
                    try {
                        long id = 100000 + userId * requestsPerUser + j;
                        String body = String.format(
                                "{\"type\": \"stress\", \"amount\": %d}",
                                (userId * requestsPerUser + j) % 10000
                        );

                        int statusCode = given()
                                .contentType(ContentType.JSON)
                                .body(body)
                                .queryParam("storage", "memory")
                                .when()
                                .put("/transaction/{id}", id)
                                .then()
                                .extract().statusCode();

                        if (statusCode == 201) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            });
            futures.add(future);
        }

        for (Future<?> future : futures) {
            future.get();
        }

        Duration duration = Duration.between(start, Instant.now());
        executor.shutdown();

        System.out.printf("Completed: %d requests in %d ms%n",
                STRESS_TEST_REQUESTS, duration.toMillis());
        System.out.printf("Success: %d, Errors: %d%n",
                successCount.get(), errorCount.get());
        System.out.printf("Throughput: %.2f req/sec%n",
                (STRESS_TEST_REQUESTS * 1000.0) / duration.toMillis());
    }

    @Test
    @DisplayName("Stress test - Mixed operations (Create, Read, Sum)")
    void stressTestMixedOperations() throws InterruptedException, ExecutionException {
        System.out.println("\n=== STRESS TEST - MIXED OPERATIONS ===");

        // Setup initial data
        setupInitialData(100);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);

        AtomicInteger creates = new AtomicInteger(0);
        AtomicInteger reads = new AtomicInteger(0);
        AtomicInteger sums = new AtomicInteger(0);

        Instant start = Instant.now();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            Future<?> future = executor.submit(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int j = 0; j < 50; j++) {
                    int operation = random.nextInt(3);
                    try {
                        switch (operation) {
                            case 0: // Create
                                long id = 200000 + userId * 1000 + j;
                                String body = String.format(
                                        "{\"type\": \"mixed\", \"amount\": %d}",
                                        random.nextInt(10000)
                                );
                                given()
                                        .contentType(ContentType.JSON)
                                        .body(body)
                                        .queryParam("storage", "memory")
                                        .when()
                                        .put("/transaction/{id}", id);
                                creates.incrementAndGet();
                                break;

                            case 1: // Read by type
                                given()
                                        .queryParam("storage", "memory")
                                        .when()
                                        .get("/types/{type}", "initial");
                                reads.incrementAndGet();
                                break;

                            case 2: // Sum
                                given()
                                        .queryParam("storage", "memory")
                                        .when()
                                        .get("/sum/{id}", random.nextInt(100));
                                sums.incrementAndGet();
                                break;
                        }
                    } catch (Exception e) {
                        // Ignore errors for stress test
                    }
                }
            });
            futures.add(future);
        }

        for (Future<?> future : futures) {
            future.get();
        }

        Duration duration = Duration.between(start, Instant.now());
        executor.shutdown();

        int totalOps = creates.get() + reads.get() + sums.get();
        System.out.printf("Completed %d operations in %d ms%n", totalOps, duration.toMillis());
        System.out.printf("Creates: %d, Reads: %d, Sums: %d%n", creates.get(), reads.get(), sums.get());
        System.out.printf("Throughput: %.2f ops/sec%n", (totalOps * 1000.0) / duration.toMillis());
    }

    @Test
    @DisplayName("Spike test - Sudden load increase")
    void spikeTest() throws InterruptedException, ExecutionException {
        System.out.println("\n=== SPIKE TEST ===");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS * 2);

        // Phase 1: Normal load (10 users)
        System.out.println("Phase 1: Normal load (10 users)...");
        List<Future<?>> phase1 = new ArrayList<>();
        Instant phase1Start = Instant.now();

        for (int i = 0; i < 10; i++) {
            final int userId = i;
            Future<?> future = executor.submit(() -> {
                for (int j = 0; j < 20; j++) {
                    createTransaction(300000 + userId * 1000 + j, "spike", 100);
                }
            });
            phase1.add(future);
        }

        for (Future<?> future : phase1) {
            future.get();
        }

        long phase1Duration = Duration.between(phase1Start, Instant.now()).toMillis();
        System.out.printf("Phase 1 completed in %d ms%n", phase1Duration);

        // Phase 2: Spike (40 users)
        System.out.println("Phase 2: Spike load (40 users)...");
        List<Future<?>> phase2 = new ArrayList<>();
        Instant phase2Start = Instant.now();

        for (int i = 0; i < 40; i++) {
            final int userId = i;
            Future<?> future = executor.submit(() -> {
                for (int j = 0; j < 20; j++) {
                    createTransaction(400000 + userId * 1000 + j, "spike", 100);
                }
            });
            phase2.add(future);
        }

        for (Future<?> future : phase2) {
            future.get();
        }

        long phase2Duration = Duration.between(phase2Start, Instant.now()).toMillis();
        System.out.printf("Phase 2 completed in %d ms%n", phase2Duration);

        executor.shutdown();

        System.out.printf("Performance degradation: %.2fx slower during spike%n",
                (double) phase2Duration / phase1Duration);
    }

    // Helper methods
    private void warmup() {
        for (int i = 0; i < WARMUP_REQUESTS; i++) {
            createTransaction(i, "warmup", 100);
        }
    }

    private long testSequentialCreates(String storage, int count) {
        Instant start = Instant.now();

        for (int i = 0; i < count; i++) {
            long id = storage.equals("memory") ? i : 50000 + i;
            createTransaction(id, "load-test", 100, storage);
        }

        return Duration.between(start, Instant.now()).toMillis();
    }

    private void setupInitialData(int count) {
        for (int i = 0; i < count; i++) {
            createTransaction(i, "initial", 1000);
        }
    }

    private void createTransaction(long id, String type, int amount) {
        createTransaction(id, type, amount, "memory");
    }

    private void createTransaction(long id, String type, int amount, String storage) {
        String body = String.format("{\"type\": \"%s\", \"amount\": %d}", type, amount);

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .queryParam("storage", storage)
                .when()
                .put("/transaction/{id}", id);
    }
}

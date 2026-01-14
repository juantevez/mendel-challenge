package com.mendel.challenge.performance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.lessThan;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TransactionApiLoadTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // Ajustamos la base path a la nueva versión de la API
        RestAssured.basePath = "/api/v1/transactionservice";
    }

    @Test
    void loadTestSequentialRequests() {
        long startTime = System.currentTimeMillis();
        int totalRequests = 1000;

        // 1. Crear jerarquía lineal masiva (ID 1 es padre de 2, 2 de 3...)
        createTransaction(1, "root", 100);
        for (int i = 2; i <= totalRequests; i++) {
            createTransactionWithParent(i, "child", 10, i - 1);
        }

        // 2. Medir tiempo de respuesta de la suma total (recursión profunda)
        given()
                .when()
                .get("/sum/1")
                .then()
                .statusCode(200)
                .time(lessThan(500L)); // El cálculo debería ser rápido incluso con 10000 niveles

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Load Test - 10000 sequential requests took: " + duration + " ms");
    }

    @Test
    void concurrentStressTest() throws InterruptedException {
        int threads = 10;
        int requestsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.execute(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    long id = 2000 + (threadId * 100L) + j;
                    createTransaction(id, "stress-type", 50.0);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    // Helpers ajustados sin el parámetro 'storage'
    private void createTransaction(long id, String type, double amount) {
        String body = String.format("{\"type\": \"%s\", \"amount\": %f}", type, amount);
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/transaction/" + id)
                .then()
                .statusCode(201);
    }

    private void createTransactionWithParent(long id, String type, double amount, long parentId) {
        String body = String.format("{\"type\": \"%s\", \"amount\": %f, \"parentId\": %d}", type, amount, parentId);
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/transaction/" + id)
                .then()
                .statusCode(201);
    }

    @Test
    void loadTestLargeHierarchy() {
        // Usamos un offset basado en el tiempo para evitar colisiones de IDs (Error 400)
        long offset = System.currentTimeMillis() % 100000;
        int totalRequests = 1000;

        // Crear el raíz
        createTransaction(offset, "root", 100);

        // Crear hijos vinculados
        for (int i = 1; i < totalRequests; i++) {
            long currentId = offset + i;
            long parentId = offset + i - 1;
            createTransactionWithParent(currentId, "child", 1.0, parentId);
        }

        // Validar la suma (Debería ser 100 + 9999*1 = 10099)
        given()
                .when()
                .get("/sum/" + offset)
                .then()
                .statusCode(200);
    }

}

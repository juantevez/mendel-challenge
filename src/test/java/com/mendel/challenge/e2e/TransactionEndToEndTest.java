package com.mendel.challenge.e2e;

import com.mendel.challenge.application.dto.TransactionRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionEndToEndTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/transactionservice";
    }

    @Test
    @DisplayName("Escenario Completo: Crear jerarquía y verificar suma y tipos")
    void fullTransactionFlow() {
        // 1. Crear una transacción raíz (ID 5000)
        TransactionRequest root = new TransactionRequest("cars", new BigDecimal("1000.0"), null);

        given()
                .contentType(ContentType.JSON)
                .body(root)
                .when()
                .put("/transaction/5000")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        // 2. Crear un hijo (ID 5001) vinculado a la raíz
        TransactionRequest child = new TransactionRequest("cars", new BigDecimal("500.0"), 5000L);

        given()
                .contentType(ContentType.JSON)
                .body(child)
                .when()
                .put("/transaction/5001")
                .then()
                .statusCode(HttpStatus.CREATED.value());

        // 3. Verificar que la suma de la raíz sea 1500.0
        given()
                .when()
                .get("/sum/5000")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("sum", equalTo(1500.0f));

        // 4. Verificar que el endpoint de tipos devuelva ambos IDs
        given()
                .when()
                .get("/types/cars")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("type", equalTo("cars"))
                .body("transactionIds", hasItems(5000, 5001))
                .body("count", equalTo(2));
    }

    @Test
    @DisplayName("Debe fallar al intentar crear una transacción con un padre inexistente")
    void shouldFailWhenParentDoesNotExist() {
        TransactionRequest orphan = new TransactionRequest("test", new BigDecimal("100.0"), 99999L);

        given()
                .contentType(ContentType.JSON)
                .body(orphan)
                .when()
                .put("/transaction/6000")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}

package com.mendel.challenge;

import com.mendel.challenge.application.dto.SumResponse;
import com.mendel.challenge.application.dto.TypeTransactionsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat; // Este es el correcto

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TransactionControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /types/{type} should return correct DTO and status")
    void shouldReturnTransactionsByType() {
        // 1. Preparar datos (usando el Service o Repositorio directamente)
        // Asumiendo que ya tienes una transacción guardada con tipo "cars"
        String type = "cars";

        // 2. Ejecutar la llamada al Controller
        ResponseEntity<TypeTransactionsResponse> response = restTemplate.getForEntity(
                "/api/v1/transactionservice/types/" + type,
                TypeTransactionsResponse.class
        );

        // 3. Verificar resultados (Esto ejecuta el método .of() y el constructor del Record)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().type()).isEqualTo(type);
        assertThat(response.getBody().count()).isNotNull();
        assertThat(response.getBody().storage()).isEqualTo("MANAGED_STORAGE");
    }

    @Test
    @DisplayName("GET /sum/{id} debería calcular la suma total de la jerarquía")
    void shouldReturnTotalSumOfHierarchy() {
        // 1. Preparar una jerarquía: Raíz (100) -> Hijo (50) -> Nieto (25) = Total 175
        long rootId = 500L;
        long childId = 501L;
        long grandchildId = 502L;

        // Crear transacciones (usando PUT para pasar por todo el flujo)
        createTransactionRequest(rootId, "parent", 100.0, null);
        createTransactionRequest(childId, "child", 50.0, rootId);
        createTransactionRequest(grandchildId, "grandchild", 25.0, childId);

        // 2. Llamar al endpoint de suma para el ID raíz
        ResponseEntity<SumResponse> response = restTemplate.getForEntity(
                "/api/v1/transactionservice/sum/" + rootId,
                SumResponse.class
        );

        // 3. Verificaciones
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Aquí validamos que el DTO TransactionSumResponse se haya construido correctamente
        // y que el valor de la suma sea exacto (175.0)
        assertThat(response.getBody().sum()).isEqualByComparingTo(new BigDecimal("175.0"));
    }

    // Helper para simplificar las llamadas PUT en el test
    private void createTransactionRequest(long id, String type, double amount, Long parentId) {
        String body = parentId == null ?
                String.format("{\"type\": \"%s\", \"amount\": %f}", type, amount) :
                String.format("{\"type\": \"%s\", \"amount\": %f, \"parentId\": %d}", type, amount, parentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(
                "/api/v1/transactionservice/transaction/" + id,
                HttpMethod.PUT,
                entity,
                Void.class
        );
    }

}
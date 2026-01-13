package com.mendel.challenge.application.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mendel.challenge.application.dto.TransactionRequest;
import com.mendel.challenge.domain.model.StorageStrategy;
import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@DisplayName("Transaction Controller Unit Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Nested
    @DisplayName("Create Transaction Tests")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should create transaction successfully with IN_MEMORY storage")
        void shouldCreateTransactionSuccessfullyWithInMemoryStorage() throws Exception {
            // Given
            Long transactionId = 1L;
            TransactionRequest request = new TransactionRequest(
                    "cars",
                    new BigDecimal("5000.00"),
                    null
            );

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .type("cars")
                    .amount(new BigDecimal("5000.00"))
                    .createdAt(Instant.now())
                    .build();

            when(transactionService.create(
                    eq(transactionId),
                    eq("cars"),
                    eq(new BigDecimal("5000.00")),
                    eq(null),
                    eq(StorageStrategy.IN_MEMORY)
            )).thenReturn(transaction);

            // When & Then
            mockMvc.perform(put("/api/v1/transactionservice/transaction/{id}", transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("storage", "memory")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(transactionId))
                    .andExpect(jsonPath("$.type").value("cars"))
                    .andExpect(jsonPath("$.amount").value(5000.00))
                    .andExpect(jsonPath("$.parentId").doesNotExist());

            verify(transactionService).create(
                    eq(transactionId),
                    eq("cars"),
                    any(BigDecimal.class),
                    eq(null),
                    eq(StorageStrategy.IN_MEMORY)
            );
        }

        @Test
        @DisplayName("Should create transaction with REDIS storage")
        void shouldCreateTransactionWithRedisStorage() throws Exception {
            // Given
            Long transactionId = 10L;
            TransactionRequest request = new TransactionRequest(
                    "electronics",
                    new BigDecimal("10000.00"),
                    null
            );

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .type("electronics")
                    .amount(new BigDecimal("10000.00"))
                    .createdAt(Instant.now())
                    .build();

            when(transactionService.create(
                    eq(transactionId),
                    eq("electronics"),
                    any(BigDecimal.class),
                    eq(null),
                    eq(StorageStrategy.REDIS)
            )).thenReturn(transaction);

            // When & Then
            mockMvc.perform(put("/api/v1/transactionservice/transaction/{id}", transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("storage", "redis")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(transactionId))
                    .andExpect(jsonPath("$.type").value("electronics"));

            verify(transactionService).create(
                    eq(transactionId),
                    eq("electronics"),
                    any(BigDecimal.class),
                    eq(null),
                    eq(StorageStrategy.REDIS)
            );
        }

        @Test
        @DisplayName("Should create transaction with parent")
        void shouldCreateTransactionWithParent() throws Exception {
            // Given
            Long transactionId = 11L;
            Long parentId = 10L;
            TransactionRequest request = new TransactionRequest(
                    "maintenance",
                    new BigDecimal("1000.00"),
                    parentId
            );

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .type("maintenance")
                    .amount(new BigDecimal("1000.00"))
                    .parentId(parentId)
                    .createdAt(Instant.now())
                    .build();

            when(transactionService.create(
                    eq(transactionId),
                    eq("maintenance"),
                    any(BigDecimal.class),
                    eq(parentId),
                    eq(StorageStrategy.IN_MEMORY)
            )).thenReturn(transaction);

            // When & Then
            mockMvc.perform(put("/api/v1/transactionservice/transaction/{id}", transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("storage", "memory")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(transactionId))
                    .andExpect(jsonPath("$.parentId").value(parentId));
        }

        @Test
        @DisplayName("Should use default storage when storage param is not provided")
        void shouldUseDefaultStorageWhenStorageParamNotProvided() throws Exception {
            // Given
            Long transactionId = 1L;
            TransactionRequest request = new TransactionRequest(
                    "test",
                    new BigDecimal("100.00"),
                    null
            );

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .type("test")
                    .amount(new BigDecimal("100.00"))
                    .createdAt(Instant.now())
                    .build();

            when(transactionService.create(
                    eq(transactionId),
                    eq("test"),
                    any(BigDecimal.class),
                    eq(null),
                    eq(StorageStrategy.IN_MEMORY)
            )).thenReturn(transaction);

            // When & Then
            mockMvc.perform(put("/api/v1/transactionservice/transaction/{id}", transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(transactionService).create(
                    eq(transactionId),
                    eq("test"),
                    any(BigDecimal.class),
                    eq(null),
                    eq(StorageStrategy.IN_MEMORY)
            );
        }

        @Test
        @DisplayName("Should return 400 when request body is invalid")
        void shouldReturn400WhenRequestBodyIsInvalid() throws Exception {
            // Given - Invalid request (missing type)
            String invalidRequest = "{\"amount\": 1000}";

            // When & Then
            mockMvc.perform(put("/api/v1/transactionservice/transaction/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("storage", "memory")
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            verify(transactionService, never()).create(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when amount is negative")
        void shouldReturn400WhenAmountIsNegative() throws Exception {
            // Given
            TransactionRequest request = new TransactionRequest(
                    "cars",
                    new BigDecimal("-100.00"),
                    null
            );

            // When & Then
            mockMvc.perform(put("/api/v1/transactionservice/transaction/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("storage", "memory")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(transactionService, never()).create(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should handle service exceptions")
        void shouldHandleServiceExceptions() throws Exception {
            // Given
            Long transactionId = 1L;
            TransactionRequest request = new TransactionRequest(
                    "cars",
                    new BigDecimal("1000.00"),
                    null
            );

            when(transactionService.create(
                    eq(transactionId),
                    eq("cars"),
                    any(BigDecimal.class),
                    eq(null),
                    eq(StorageStrategy.IN_MEMORY)
            )).thenThrow(new IllegalArgumentException("Transaction already exists"));

            // When & Then
            mockMvc.perform(put("/api/v1/transactionservice/transaction/{id}", transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("storage", "memory")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Transactions By Type Tests")
    class GetTransactionsByTypeTests {

        @Test
        @DisplayName("Should get transactions by type successfully")
        void shouldGetTransactionsByTypeSuccessfully() throws Exception {
            // Given
            String type = "cars";
            List<Transaction> transactions = Arrays.asList(
                    Transaction.builder().id(1L).type(type).amount(new BigDecimal("1000")).build(),
                    Transaction.builder().id(2L).type(type).amount(new BigDecimal("2000")).build()
            );

            when(transactionService.getByType(eq(type), eq(StorageStrategy.IN_MEMORY)))
                    .thenReturn(transactions);

            // When & Then
            mockMvc.perform(get("/api/v1/transactionservice/types/{type}", type)
                            .param("storage", "memory"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value(type))
                    .andExpect(jsonPath("$.transactionIds", hasSize(2)))
                    .andExpect(jsonPath("$.transactionIds", containsInAnyOrder(1, 2)))
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.storage").value("memory"));

            verify(transactionService).getByType(eq(type), eq(StorageStrategy.IN_MEMORY));
        }

        @Test
        @DisplayName("Should return empty list for non-existent type")
        void shouldReturnEmptyListForNonExistentType() throws Exception {
            // Given
            String type = "nonexistent";
            when(transactionService.getByType(eq(type), eq(StorageStrategy.IN_MEMORY)))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/v1/transactionservice/types/{type}", type)
                            .param("storage", "memory"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value(type))
                    .andExpect(jsonPath("$.transactionIds", hasSize(0)))
                    .andExpect(jsonPath("$.count").value(0))
                    .andExpect(jsonPath("$.storage").value("memory"));
        }

        @Test
        @DisplayName("Should work with REDIS storage")
        void shouldWorkWithRedisStorage() throws Exception {
            // Given
            String type = "electronics";
            List<Transaction> transactions = Arrays.asList(
                    Transaction.builder().id(10L).type(type).amount(new BigDecimal("10000")).build()
            );

            when(transactionService.getByType(eq(type), eq(StorageStrategy.REDIS)))
                    .thenReturn(transactions);

            // When & Then
            mockMvc.perform(get("/api/v1/transactionservice/types/{type}", type)
                            .param("storage", "redis"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transactionIds", hasSize(1)))
                    .andExpect(jsonPath("$.storage").value("redis"));
        }

        @Test
        @DisplayName("Should use default storage when not specified")
        void shouldUseDefaultStorageWhenNotSpecified() throws Exception {
            // Given
            String type = "test";
            when(transactionService.getByType(eq(type), eq(StorageStrategy.IN_MEMORY)))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/v1/transactionservice/types/{type}", type))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.storage").value("memory"));

            verify(transactionService).getByType(eq(type), eq(StorageStrategy.IN_MEMORY));
        }
    }

    @Nested
    @DisplayName("Get Transaction Sum Tests")
    class GetTransactionSumTests {

        @Test
        @DisplayName("Should calculate sum successfully")
        void shouldCalculateSumSuccessfully() throws Exception {
            // Given
            Long transactionId = 1L;
            BigDecimal sum = new BigDecimal("6000.00");

            when(transactionService.calculateSum(eq(transactionId), eq(StorageStrategy.IN_MEMORY)))
                    .thenReturn(sum);

            // When & Then
            mockMvc.perform(get("/api/v1/transactionservice/sum/{id}", transactionId)
                            .param("storage", "memory"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sum").value(6000.00));

            verify(transactionService).calculateSum(eq(transactionId), eq(StorageStrategy.IN_MEMORY));
        }

        @Test
        @DisplayName("Should calculate sum with REDIS storage")
        void shouldCalculateSumWithRedisStorage() throws Exception {
            // Given
            Long transactionId = 10L;
            BigDecimal sum = new BigDecimal("15000.00");

            when(transactionService.calculateSum(eq(transactionId), eq(StorageStrategy.REDIS)))
                    .thenReturn(sum);

            // When & Then
            mockMvc.perform(get("/api/v1/transactionservice/sum/{id}", transactionId)
                            .param("storage", "redis"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sum").value(15000.00));
        }

        @Test
        @DisplayName("Should handle transaction not found")
        void shouldHandleTransactionNotFound() throws Exception {
            // Given
            Long transactionId = 999L;

            when(transactionService.calculateSum(eq(transactionId), eq(StorageStrategy.IN_MEMORY)))
                    .thenThrow(new IllegalArgumentException("Transaction not found"));

            // When & Then
            mockMvc.perform(get("/api/v1/transactionservice/sum/{id}", transactionId)
                            .param("storage", "memory"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should use default storage when not specified")
        void shouldUseDefaultStorageWhenNotSpecified() throws Exception {
            // Given
            Long transactionId = 1L;
            BigDecimal sum = new BigDecimal("1000.00");

            when(transactionService.calculateSum(eq(transactionId), eq(StorageStrategy.IN_MEMORY)))
                    .thenReturn(sum);

            // When & Then
            mockMvc.perform(get("/api/v1/transactionservice/sum/{id}", transactionId))
                    .andExpect(status().isOk());

            verify(transactionService).calculateSum(eq(transactionId), eq(StorageStrategy.IN_MEMORY));
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should correctly map transaction to response")
        void shouldCorrectlyMapTransactionToResponse() throws Exception {
            // Given
            Long transactionId = 1L;
            Instant now = Instant.now();
            TransactionRequest request = new TransactionRequest(
                    "cars",
                    new BigDecimal("5000.00"),
                    null
            );

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .type("cars")
                    .amount(new BigDecimal("5000.00"))
                    .createdAt(now)
                    .build();

            when(transactionService.create(any(), any(), any(), any(), any()))
                    .thenReturn(transaction);

            // When & Then
            mockMvc.perform(put("/api/v1/transactionservice/transaction/{id}", transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("storage", "memory")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(transactionId))
                    .andExpect(jsonPath("$.type").value("cars"))
                    .andExpect(jsonPath("$.amount").value(5000.00))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("Should include parentId in response when present")
        void shouldIncludeParentIdInResponseWhenPresent() throws Exception {
            // Given
            Long transactionId = 2L;
            Long parentId = 1L;
            TransactionRequest request = new TransactionRequest(
                    "maintenance",
                    new BigDecimal("500.00"),
                    parentId
            );

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .type("maintenance")
                    .amount(new BigDecimal("500.00"))
                    .parentId(parentId)
                    .createdAt(Instant.now())
                    .build();

            when(transactionService.create(any(), any(), any(), any(), any()))
                    .thenReturn(transaction);

            // When & Then
            mockMvc.perform(put("/api/v1/transactionservice/transaction/{id}", transactionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .param("storage", "memory")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.parentId").value(parentId));
        }
    }
}

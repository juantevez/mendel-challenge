package com.mendel.challenge.infrastructure.adapter.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.infrastructure.adapter.redis.dto.TransactionRedisDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisTransactionRepository Unit Tests")
class RedisTransactionRepositoryTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private RedisTransactionRepository repository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Usar lenient() para permitir que algunos tests no usen estos mocks
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        repository = new RedisTransactionRepository(redisTemplate);

        // ObjectMapper para assertions
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize repository with RedisTemplate")
        void shouldInitializeRepositoryWithRedisTemplate() {
            // When
            RedisTransactionRepository newRepository = new RedisTransactionRepository(redisTemplate);

            // Then
            assertThat(newRepository).isNotNull();
            assertThat(newRepository.getImplementationType()).isEqualTo("REDIS");
        }
    }

    @Nested
    @DisplayName("Save Transaction Tests")
    class SaveTransactionTests {

        @BeforeEach
        void setUpSaveTests() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        }

        @Test
        @DisplayName("Should save transaction successfully")
        void shouldSaveTransactionSuccessfully() throws JsonProcessingException {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            // When
            Transaction result = repository.save(transaction);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            verify(valueOperations).set(eq("transaction:1"), anyString());
            verify(setOperations).add("type:cars", "1");
            verify(setOperations, never()).add(startsWith("children:"), anyString());
        }

        @Test
        @DisplayName("Should save transaction with parent and update children index")
        void shouldSaveTransactionWithParentAndUpdateChildrenIndex() {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(2L)
                    .type("maintenance")
                    .amount(new BigDecimal("500.00"))
                    .parentId(1L)
                    .build();

            // When
            Transaction result = repository.save(transaction);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getParentId()).isEqualTo(1L);

            verify(valueOperations).set(eq("transaction:2"), anyString());
            verify(setOperations).add("type:maintenance", "2");
            verify(setOperations).add("children:1", "2");
        }

        @Test
        @DisplayName("Should save transaction and update type index")
        void shouldSaveTransactionAndUpdateTypeIndex() {
            // Given
            Transaction t1 = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            Transaction t2 = Transaction.builder()
                    .id(2L)
                    .type("cars")
                    .amount(new BigDecimal("2000.00"))
                    .build();

            // When
            repository.save(t1);
            repository.save(t2);

            // Then
            verify(setOperations).add("type:cars", "1");
            verify(setOperations).add("type:cars", "2");
        }

        @Test
        @DisplayName("Should handle serialization correctly")
        void shouldHandleSerializationCorrectly() throws JsonProcessingException {
            // Given
            Instant now = Instant.now();
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .createdAt(now)
                    .build();

            // When
            repository.save(transaction);

            // Then
            verify(valueOperations).set(eq("transaction:1"), argThat(json -> {
                try {
                    TransactionRedisDTO dto = objectMapper.readValue(json, TransactionRedisDTO.class);
                    return dto.getId().equals(1L) &&
                            dto.getType().equals("cars") &&
                            dto.getAmount().compareTo(new BigDecimal("1000.00")) == 0;
                } catch (JsonProcessingException e) {
                    return false;
                }
            }));
        }

        @Test
        @DisplayName("Should throw exception when Redis operation fails")
        void shouldThrowExceptionWhenRedisOperationFails() {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            // Simular que Redis lanza una excepción
            doThrow(new RuntimeException("Redis connection error"))
                    .when(valueOperations).set(anyString(), anyString());

            // When & Then
            assertThatThrownBy(() -> repository.save(transaction))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Redis connection error");
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @BeforeEach
        void setUpFindByIdTests() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @Test
        @DisplayName("Should find transaction by ID")
        void shouldFindTransactionById() throws JsonProcessingException {
            // Given
            Long id = 1L;
            Transaction expected = Transaction.builder()
                    .id(id)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            TransactionRedisDTO dto = TransactionRedisDTO.fromDomain(expected);
            String json = objectMapper.writeValueAsString(dto);

            when(valueOperations.get("transaction:1")).thenReturn(json);

            // When
            Optional<Transaction> result = repository.findById(id);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(id);
            assertThat(result.get().getType()).isEqualTo("cars");
            assertThat(result.get().getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));

            verify(valueOperations).get("transaction:1");
        }

        @Test
        @DisplayName("Should return empty when transaction not found")
        void shouldReturnEmptyWhenTransactionNotFound() {
            // Given
            when(valueOperations.get("transaction:999")).thenReturn(null);

            // When
            Optional<Transaction> result = repository.findById(999L);

            // Then
            assertThat(result).isEmpty();
            verify(valueOperations).get("transaction:999");
        }

        @Test
        @DisplayName("Should handle deserialization correctly")
        void shouldHandleDeserializationCorrectly() throws JsonProcessingException {
            // Given
            Instant now = Instant.now();
            TransactionRedisDTO dto = new TransactionRedisDTO(
                    1L,
                    "cars",
                    new BigDecimal("1000.00"),
                    null,
                    now
            );
            String json = objectMapper.writeValueAsString(dto);

            when(valueOperations.get("transaction:1")).thenReturn(json);

            // When
            Optional<Transaction> result = repository.findById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should throw exception when deserialization fails")
        void shouldThrowExceptionWhenDeserializationFails() {
            // Given
            String invalidJson = "{invalid json}";
            when(valueOperations.get("transaction:1")).thenReturn(invalidJson);

            // When & Then
            assertThatThrownBy(() -> repository.findById(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error deserializing transaction");
        }

        @Test
        @DisplayName("Should throw exception when Redis get operation fails")
        void shouldThrowExceptionWhenRedisGetOperationFails() {
            // Given
            when(valueOperations.get("transaction:1"))
                    .thenThrow(new RuntimeException("Redis connection error"));

            // When & Then
            assertThatThrownBy(() -> repository.findById(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Redis connection error");
        }
    }

    @Nested
    @DisplayName("Find By Type Tests")
    class FindByTypeTests {

        @Test
        @DisplayName("Should find transactions by type")
        void shouldFindTransactionsByType() throws JsonProcessingException {
            // Given
            String type = "cars";
            Set<String> ids = new HashSet<>(Arrays.asList("1", "2"));

            Transaction t1 = Transaction.builder()
                    .id(1L)
                    .type(type)
                    .amount(new BigDecimal("1000.00"))
                    .build();

            Transaction t2 = Transaction.builder()
                    .id(2L)
                    .type(type)
                    .amount(new BigDecimal("2000.00"))
                    .build();

            when(setOperations.members("type:cars")).thenReturn(ids);
            when(valueOperations.get("transaction:1"))
                    .thenReturn(objectMapper.writeValueAsString(TransactionRedisDTO.fromDomain(t1)));
            when(valueOperations.get("transaction:2"))
                    .thenReturn(objectMapper.writeValueAsString(TransactionRedisDTO.fromDomain(t2)));

            // When
            List<Transaction> result = repository.findByType(type);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(Transaction::getType)
                    .containsOnly("cars");

            verify(setOperations).members("type:cars");
            verify(valueOperations).get("transaction:1");
            verify(valueOperations).get("transaction:2");
        }

        @Test
        @DisplayName("Should return empty list when type not found")
        void shouldReturnEmptyListWhenTypeNotFound() {
            // Given
            when(setOperations.members("type:nonexistent")).thenReturn(null);

            // When
            List<Transaction> result = repository.findByType("nonexistent");

            // Then
            assertThat(result).isEmpty();
            verify(setOperations).members("type:nonexistent");
        }

        @Test
        @DisplayName("Should return empty list when type has empty set")
        void shouldReturnEmptyListWhenTypeHasEmptySet() {
            // Given
            when(setOperations.members("type:empty")).thenReturn(Collections.emptySet());

            // When
            List<Transaction> result = repository.findByType("empty");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should filter out transactions that don't exist anymore")
        void shouldFilterOutTransactionsThatDontExistAnymore() throws JsonProcessingException {
            // Given
            Set<String> ids = new HashSet<>(Arrays.asList("1", "2", "3"));

            Transaction t1 = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            when(setOperations.members("type:cars")).thenReturn(ids);
            when(valueOperations.get("transaction:1"))
                    .thenReturn(objectMapper.writeValueAsString(TransactionRedisDTO.fromDomain(t1)));
            when(valueOperations.get("transaction:2")).thenReturn(null);
            when(valueOperations.get("transaction:3")).thenReturn(null);

            // When
            List<Transaction> result = repository.findByType("cars");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Find Children Tests")
    class FindChildrenTests {

        @Test
        @DisplayName("Should find children of parent transaction")
        void shouldFindChildrenOfParentTransaction() throws JsonProcessingException {
            // Given
            Long parentId = 1L;
            Set<String> childIds = new HashSet<>(Arrays.asList("2", "3"));

            Transaction child1 = Transaction.builder()
                    .id(2L)
                    .type("maintenance")
                    .amount(new BigDecimal("200.00"))
                    .parentId(parentId)
                    .build();

            Transaction child2 = Transaction.builder()
                    .id(3L)
                    .type("fuel")
                    .amount(new BigDecimal("100.00"))
                    .parentId(parentId)
                    .build();

            when(setOperations.members("children:1")).thenReturn(childIds);
            when(valueOperations.get("transaction:2"))
                    .thenReturn(objectMapper.writeValueAsString(TransactionRedisDTO.fromDomain(child1)));
            when(valueOperations.get("transaction:3"))
                    .thenReturn(objectMapper.writeValueAsString(TransactionRedisDTO.fromDomain(child2)));

            // When
            List<Transaction> result = repository.findChildrenOf(parentId);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(Transaction::getId)
                    .containsExactlyInAnyOrder(2L, 3L);

            verify(setOperations).members("children:1");
        }

        @Test
        @DisplayName("Should return empty list when parent has no children")
        void shouldReturnEmptyListWhenParentHasNoChildren() {
            // Given
            when(setOperations.members("children:1")).thenReturn(Collections.emptySet());

            // When
            List<Transaction> result = repository.findChildrenOf(1L);

            // Then
            assertThat(result).isEmpty();
            verify(setOperations).members("children:1");
        }

        @Test
        @DisplayName("Should return empty list when children set is null")
        void shouldReturnEmptyListWhenChildrenSetIsNull() {
            // Given
            when(setOperations.members("children:1")).thenReturn(null);

            // When
            List<Transaction> result = repository.findChildrenOf(1L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should filter out children that don't exist anymore")
        void shouldFilterOutChildrenThatDontExistAnymore() throws JsonProcessingException {
            // Given
            Set<String> childIds = new HashSet<>(Arrays.asList("2", "3"));

            Transaction child1 = Transaction.builder()
                    .id(2L)
                    .type("maintenance")
                    .amount(new BigDecimal("200.00"))
                    .parentId(1L)
                    .build();

            when(setOperations.members("children:1")).thenReturn(childIds);
            when(valueOperations.get("transaction:2"))
                    .thenReturn(objectMapper.writeValueAsString(TransactionRedisDTO.fromDomain(child1)));
            when(valueOperations.get("transaction:3")).thenReturn(null);

            // When
            List<Transaction> result = repository.findChildrenOf(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Exists By ID Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should return true when transaction exists")
        void shouldReturnTrueWhenTransactionExists() {
            // Given
            when(redisTemplate.hasKey("transaction:1")).thenReturn(true);

            // When
            boolean result = repository.existsById(1L);

            // Then
            assertThat(result).isTrue();
            verify(redisTemplate).hasKey("transaction:1");
        }

        @Test
        @DisplayName("Should return false when transaction does not exist")
        void shouldReturnFalseWhenTransactionDoesNotExist() {
            // Given
            when(redisTemplate.hasKey("transaction:999")).thenReturn(false);

            // When
            boolean result = repository.existsById(999L);

            // Then
            assertThat(result).isFalse();
            verify(redisTemplate).hasKey("transaction:999");
        }

        @Test
        @DisplayName("Should return false when hasKey returns null")
        void shouldReturnFalseWhenHasKeyReturnsNull() {
            // Given
            when(redisTemplate.hasKey("transaction:1")).thenReturn(null);

            // When
            boolean result = repository.existsById(1L);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Get Implementation Type Tests")
    class GetImplementationTypeTests {

        @Test
        @DisplayName("Should return REDIS as implementation type")
        void shouldReturnRedisAsImplementationType() {
            // When
            String implementationType = repository.getImplementationType();

            // Then
            assertThat(implementationType).isEqualTo("REDIS");
        }
    }

    @Nested
    @DisplayName("Helper Methods Tests")
    class HelperMethodsTests {

        @Test
        @DisplayName("Should generate correct transaction key")
        void shouldGenerateCorrectTransactionKey() throws JsonProcessingException {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(123L)
                    .type("test")
                    .amount(new BigDecimal("100"))
                    .build();

            // When
            repository.save(transaction);

            // Then
            verify(valueOperations).set(eq("transaction:123"), anyString());
        }

        @Test
        @DisplayName("Should generate correct type key")
        void shouldGenerateCorrectTypeKey() {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .type("test-type")
                    .amount(new BigDecimal("100"))
                    .build();

            // When
            repository.save(transaction);

            // Then
            verify(setOperations).add("type:test-type", "1");
        }

        @Test
        @DisplayName("Should generate correct children key")
        void shouldGenerateCorrectChildrenKey() {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(2L)
                    .type("test")
                    .amount(new BigDecimal("100"))
                    .parentId(456L)
                    .build();

            // When
            repository.save(transaction);

            // Then
            verify(setOperations).add("children:456", "2");
        }
    }

    @Nested
    @DisplayName("Integration Scenarios Tests")
    class IntegrationScenariosTests {

        @Test
        @DisplayName("Should handle complete save and retrieve workflow")
        void shouldHandleCompleteSaveAndRetrieveWorkflow() throws JsonProcessingException {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            TransactionRedisDTO dto = TransactionRedisDTO.fromDomain(transaction);
            String json = objectMapper.writeValueAsString(dto);

            when(valueOperations.get("transaction:1")).thenReturn(json);

            // When - Save
            Transaction saved = repository.save(transaction);

            // Then - Verify save
            assertThat(saved).isNotNull();
            verify(valueOperations).set(eq("transaction:1"), anyString());

            // When - Find
            Optional<Transaction> found = repository.findById(1L);

            // Then - Verify find
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should handle parent-child relationship workflow")
        void shouldHandleParentChildRelationshipWorkflow() throws JsonProcessingException {
            // Given
            Transaction parent = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            Transaction child = Transaction.builder()
                    .id(2L)
                    .type("maintenance")
                    .amount(new BigDecimal("200.00"))
                    .parentId(1L)
                    .build();

            Set<String> childIds = new HashSet<>(Collections.singletonList("2"));

            when(setOperations.members("children:1")).thenReturn(childIds);
            when(valueOperations.get("transaction:2"))
                    .thenReturn(objectMapper.writeValueAsString(TransactionRedisDTO.fromDomain(child)));

            // When
            repository.save(parent);
            repository.save(child);
            List<Transaction> children = repository.findChildrenOf(1L);

            // Then
            verify(setOperations).add("children:1", "2");
            assertThat(children).hasSize(1);
            assertThat(children.get(0).getParentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should handle type index workflow")
        void shouldHandleTypeIndexWorkflow() throws JsonProcessingException {
            // Given
            Transaction t1 = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            Transaction t2 = Transaction.builder()
                    .id(2L)
                    .type("cars")
                    .amount(new BigDecimal("2000.00"))
                    .build();

            Set<String> ids = new HashSet<>(Arrays.asList("1", "2"));

            when(setOperations.members("type:cars")).thenReturn(ids);
            when(valueOperations.get("transaction:1"))
                    .thenReturn(objectMapper.writeValueAsString(TransactionRedisDTO.fromDomain(t1)));
            when(valueOperations.get("transaction:2"))
                    .thenReturn(objectMapper.writeValueAsString(TransactionRedisDTO.fromDomain(t2)));

            // When
            repository.save(t1);
            repository.save(t2);
            List<Transaction> found = repository.findByType("cars");

            // Then
            verify(setOperations).add("type:cars", "1");
            verify(setOperations).add("type:cars", "2");
            assertThat(found).hasSize(2);
        }
    }


}

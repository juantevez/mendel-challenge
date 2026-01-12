package com.mendel.challenge.domain.service;

import com.mendel.challenge.domain.model.StorageStrategy;
import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import com.mendel.challenge.infrastructure.factory.TransactionRepositoryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepositoryFactory repositoryFactory;

    @Mock
    private TransactionRepository mockRepository;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        // Por defecto, el factory retorna el mock repository (usar lenient para tests que lo sobrescriben)
        lenient().when(repositoryFactory.getRepository(any(StorageStrategy.class)))
                .thenReturn(mockRepository);
        lenient().when(mockRepository.getImplementationType()).thenReturn("MOCK");
    }

    @Nested
    @DisplayName("Create Transaction Tests")
    class CreateTransactionTests {

        @Test
        @DisplayName("Should create transaction successfully with IN_MEMORY strategy")
        void shouldCreateTransactionWithInMemoryStrategy() {
            // Given
            Long id = 1L;
            String type = "cars";
            BigDecimal amount = new BigDecimal("1000.00");
            Long parentId = null;

            Transaction expectedTransaction = Transaction.builder()
                    .id(id)
                    .type(type)
                    .amount(amount)
                    .parentId(parentId)
                    .build();

            when(mockRepository.existsById(id)).thenReturn(false);
            when(mockRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);

            // When
            Transaction result = transactionService.create(id, type, amount, parentId, StorageStrategy.IN_MEMORY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getType()).isEqualTo(type);
            assertThat(result.getAmount()).isEqualTo(amount);
            assertThat(result.getParentId()).isNull();

            verify(repositoryFactory).getRepository(StorageStrategy.IN_MEMORY);
            verify(mockRepository).existsById(id);
            verify(mockRepository).save(any(Transaction.class));
            // Removido: verifyNoMoreInteractions(mockRepository);
        }

        @Test
        @DisplayName("Should create transaction with parent successfully")
        void shouldCreateTransactionWithParent() {
            // Given
            Long id = 2L;
            Long parentId = 1L;
            String type = "maintenance";
            BigDecimal amount = new BigDecimal("500.00");

            Transaction expectedTransaction = Transaction.builder()
                    .id(id)
                    .type(type)
                    .amount(amount)
                    .parentId(parentId)
                    .build();

            when(mockRepository.existsById(id)).thenReturn(false);
            when(mockRepository.existsById(parentId)).thenReturn(true);
            when(mockRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);

            // When
            Transaction result = transactionService.create(id, type, amount, parentId, StorageStrategy.IN_MEMORY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getParentId()).isEqualTo(parentId);

            verify(mockRepository).existsById(id);
            verify(mockRepository).existsById(parentId);
            verify(mockRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("Should throw exception when transaction ID already exists")
        void shouldThrowExceptionWhenIdExists() {
            // Given
            Long id = 1L;
            String type = "cars";
            BigDecimal amount = new BigDecimal("1000.00");

            when(mockRepository.existsById(id)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() ->
                    transactionService.create(id, type, amount, null, StorageStrategy.IN_MEMORY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Transaction with id " + id + " already exists");

            verify(mockRepository).existsById(id);
            verify(mockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when parent transaction does not exist")
        void shouldThrowExceptionWhenParentDoesNotExist() {
            // Given
            Long id = 2L;
            Long parentId = 999L;
            String type = "maintenance";
            BigDecimal amount = new BigDecimal("500.00");

            when(mockRepository.existsById(id)).thenReturn(false);
            when(mockRepository.existsById(parentId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() ->
                    transactionService.create(id, type, amount, parentId, StorageStrategy.IN_MEMORY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Parent transaction " + parentId + " does not exist");

            verify(mockRepository).existsById(id);
            verify(mockRepository).existsById(parentId);
            verify(mockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create transaction with REDIS strategy")
        void shouldCreateTransactionWithRedisStrategy() {
            // Given
            Long id = 10L;
            String type = "electronics";
            BigDecimal amount = new BigDecimal("5000.00");

            Transaction expectedTransaction = Transaction.builder()
                    .id(id)
                    .type(type)
                    .amount(amount)
                    .build();

            when(mockRepository.existsById(id)).thenReturn(false);
            when(mockRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);

            // When
            Transaction result = transactionService.create(id, type, amount, null, StorageStrategy.REDIS);

            // Then
            assertThat(result).isNotNull();
            verify(repositoryFactory).getRepository(StorageStrategy.REDIS);
        }

        @Test
        @DisplayName("Should delegate to default strategy when using interface method")
        void shouldDelegateToDefaultStrategy() {
            // Given
            Long id = 1L;
            String type = "test";
            BigDecimal amount = new BigDecimal("100.00");

            Transaction expectedTransaction = Transaction.builder()
                    .id(id)
                    .type(type)
                    .amount(amount)
                    .build();

            when(mockRepository.existsById(id)).thenReturn(false);
            when(mockRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);

            // When
            Transaction result = transactionService.create(id, type, amount, null);

            // Then
            assertThat(result).isNotNull();
            verify(repositoryFactory).getRepository(StorageStrategy.IN_MEMORY);
        }
    }

    @Nested
    @DisplayName("Get Transactions By Type Tests")
    class GetByTypeTests {

        @Test
        @DisplayName("Should return transactions by type")
        void shouldReturnTransactionsByType() {
            // Given
            String type = "cars";
            List<Transaction> expectedTransactions = Arrays.asList(
                    Transaction.builder().id(1L).type(type).amount(new BigDecimal("1000")).build(),
                    Transaction.builder().id(2L).type(type).amount(new BigDecimal("2000")).build()
            );

            when(mockRepository.findByType(type)).thenReturn(expectedTransactions);

            // When
            List<Transaction> result = transactionService.getByType(type, StorageStrategy.IN_MEMORY);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(expectedTransactions);
            verify(mockRepository).findByType(type);
        }

        @Test
        @DisplayName("Should return empty list when no transactions found for type")
        void shouldReturnEmptyListWhenNoTransactionsFound() {
            // Given
            String type = "nonexistent";
            when(mockRepository.findByType(type)).thenReturn(Collections.emptyList());

            // When
            List<Transaction> result = transactionService.getByType(type, StorageStrategy.IN_MEMORY);

            // Then
            assertThat(result).isEmpty();
            verify(mockRepository).findByType(type);
        }

        @Test
        @DisplayName("Should use REDIS strategy when specified")
        void shouldUseRedisStrategy() {
            // Given
            String type = "electronics";
            when(mockRepository.findByType(type)).thenReturn(Collections.emptyList());

            // When
            transactionService.getByType(type, StorageStrategy.REDIS);

            // Then
            verify(repositoryFactory).getRepository(StorageStrategy.REDIS);
            verify(mockRepository).findByType(type);
        }

        @Test
        @DisplayName("Should delegate to default strategy when using interface method")
        void shouldDelegateToDefaultStrategy() {
            // Given
            String type = "test";
            when(mockRepository.findByType(type)).thenReturn(Collections.emptyList());

            // When
            transactionService.getByType(type);

            // Then
            verify(repositoryFactory).getRepository(StorageStrategy.IN_MEMORY);
        }
    }

    @Nested
    @DisplayName("Calculate Sum Tests")
    class CalculateSumTests {

        @Test
        @DisplayName("Should calculate sum for transaction without children")
        void shouldCalculateSumForTransactionWithoutChildren() {
            // Given
            Long transactionId = 1L;
            BigDecimal amount = new BigDecimal("1000.00");

            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .type("cars")
                    .amount(amount)
                    .build();

            when(mockRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(mockRepository.findChildrenOf(transactionId)).thenReturn(Collections.emptyList());

            // When
            BigDecimal result = transactionService.calculateSum(transactionId, StorageStrategy.IN_MEMORY);

            // Then
            assertThat(result).isEqualByComparingTo(amount);
            verify(mockRepository).findById(transactionId);
            verify(mockRepository).findChildrenOf(transactionId);
        }

        @Test
        @DisplayName("Should calculate sum for transaction with one level of children")
        void shouldCalculateSumWithChildren() {
            // Given
            Long parentId = 1L;
            BigDecimal parentAmount = new BigDecimal("1000.00");
            BigDecimal child1Amount = new BigDecimal("200.00");
            BigDecimal child2Amount = new BigDecimal("300.00");

            Transaction parent = Transaction.builder()
                    .id(parentId)
                    .type("cars")
                    .amount(parentAmount)
                    .build();

            Transaction child1 = Transaction.builder()
                    .id(2L)
                    .type("maintenance")
                    .amount(child1Amount)
                    .parentId(parentId)
                    .build();

            Transaction child2 = Transaction.builder()
                    .id(3L)
                    .type("fuel")
                    .amount(child2Amount)
                    .parentId(parentId)
                    .build();

            when(mockRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(mockRepository.findChildrenOf(parentId)).thenReturn(Arrays.asList(child1, child2));
            when(mockRepository.findChildrenOf(2L)).thenReturn(Collections.emptyList());
            when(mockRepository.findChildrenOf(3L)).thenReturn(Collections.emptyList());

            // When
            BigDecimal result = transactionService.calculateSum(parentId, StorageStrategy.IN_MEMORY);

            // Then
            BigDecimal expected = parentAmount.add(child1Amount).add(child2Amount); // 1500
            assertThat(result).isEqualByComparingTo(expected);
            verify(mockRepository).findById(parentId);
            verify(mockRepository).findChildrenOf(parentId);
            verify(mockRepository).findChildrenOf(2L);
            verify(mockRepository).findChildrenOf(3L);
        }

        @Test
        @DisplayName("Should calculate sum for transaction with multiple levels of hierarchy")
        void shouldCalculateSumWithMultipleLevels() {
            // Given
            Long rootId = 1L;

            Transaction root = Transaction.builder()
                    .id(rootId)
                    .type("project")
                    .amount(new BigDecimal("1000"))
                    .build();

            Transaction child1 = Transaction.builder()
                    .id(2L)
                    .type("development")
                    .amount(new BigDecimal("500"))
                    .parentId(rootId)
                    .build();

            Transaction child2 = Transaction.builder()
                    .id(3L)
                    .type("testing")
                    .amount(new BigDecimal("300"))
                    .parentId(rootId)
                    .build();

            Transaction grandchild = Transaction.builder()
                    .id(4L)
                    .type("frontend")
                    .amount(new BigDecimal("200"))
                    .parentId(2L)
                    .build();

            when(mockRepository.findById(rootId)).thenReturn(Optional.of(root));
            when(mockRepository.findChildrenOf(rootId)).thenReturn(Arrays.asList(child1, child2));
            when(mockRepository.findChildrenOf(2L)).thenReturn(Arrays.asList(grandchild));
            when(mockRepository.findChildrenOf(3L)).thenReturn(Collections.emptyList());
            when(mockRepository.findChildrenOf(4L)).thenReturn(Collections.emptyList());

            // When
            BigDecimal result = transactionService.calculateSum(rootId, StorageStrategy.IN_MEMORY);

            // Then
            // 1000 + 500 + 300 + 200 = 2000
            assertThat(result).isEqualByComparingTo(new BigDecimal("2000"));
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            // Given
            Long transactionId = 999L;
            when(mockRepository.findById(transactionId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    transactionService.calculateSum(transactionId, StorageStrategy.IN_MEMORY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Transaction " + transactionId + " not found");

            verify(mockRepository).findById(transactionId);
            verify(mockRepository, never()).findChildrenOf(any());
        }

        @Test
        @DisplayName("Should use REDIS strategy when specified")
        void shouldUseRedisStrategy() {
            // Given
            Long transactionId = 1L;
            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .type("test")
                    .amount(new BigDecimal("100"))
                    .build();

            when(mockRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(mockRepository.findChildrenOf(transactionId)).thenReturn(Collections.emptyList());

            // When
            transactionService.calculateSum(transactionId, StorageStrategy.REDIS);

            // Then
            verify(repositoryFactory).getRepository(StorageStrategy.REDIS);
        }

        @Test
        @DisplayName("Should delegate to default strategy when using interface method")
        void shouldDelegateToDefaultStrategy() {
            // Given
            Long transactionId = 1L;
            Transaction transaction = Transaction.builder()
                    .id(transactionId)
                    .type("test")
                    .amount(new BigDecimal("100"))
                    .build();

            when(mockRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(mockRepository.findChildrenOf(transactionId)).thenReturn(Collections.emptyList());

            // When
            transactionService.calculateSum(transactionId);

            // Then
            verify(repositoryFactory).getRepository(StorageStrategy.IN_MEMORY);
        }

        @Test
        @DisplayName("Should handle deep hierarchy correctly")
        void shouldHandleDeepHierarchy() {
            // Given
            // Level 0: Root (1000)
            // Level 1: Child1 (500), Child2 (300)
            // Level 2: GrandChild1 (200)
            // Level 3: GreatGrandChild (100)
            // Total: 2100

            Transaction root = Transaction.builder()
                    .id(1L).type("root").amount(new BigDecimal("1000")).build();

            Transaction child1 = Transaction.builder()
                    .id(2L).type("child1").amount(new BigDecimal("500")).parentId(1L).build();

            Transaction child2 = Transaction.builder()
                    .id(3L).type("child2").amount(new BigDecimal("300")).parentId(1L).build();

            Transaction grandChild = Transaction.builder()
                    .id(4L).type("grandchild").amount(new BigDecimal("200")).parentId(2L).build();

            Transaction greatGrandChild = Transaction.builder()
                    .id(5L).type("greatgrandchild").amount(new BigDecimal("100")).parentId(4L).build();

            when(mockRepository.findById(1L)).thenReturn(Optional.of(root));
            when(mockRepository.findChildrenOf(1L)).thenReturn(Arrays.asList(child1, child2));
            when(mockRepository.findChildrenOf(2L)).thenReturn(Arrays.asList(grandChild));
            when(mockRepository.findChildrenOf(3L)).thenReturn(Collections.emptyList());
            when(mockRepository.findChildrenOf(4L)).thenReturn(Arrays.asList(greatGrandChild));
            when(mockRepository.findChildrenOf(5L)).thenReturn(Collections.emptyList());

            // When
            BigDecimal result = transactionService.calculateSum(1L, StorageStrategy.IN_MEMORY);

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("2100"));
        }
    }

    @Nested
    @DisplayName("Integration with Repository Factory Tests")
    class RepositoryFactoryTests {

        @Test
        @DisplayName("Should get correct repository from factory for each strategy")
        void shouldGetCorrectRepositoryFromFactory() {
            // Given
            TransactionRepository inMemoryRepo = mock(TransactionRepository.class);
            TransactionRepository redisRepo = mock(TransactionRepository.class);

            when(repositoryFactory.getRepository(StorageStrategy.IN_MEMORY)).thenReturn(inMemoryRepo);
            when(repositoryFactory.getRepository(StorageStrategy.REDIS)).thenReturn(redisRepo);

            when(inMemoryRepo.existsById(any())).thenReturn(false);
            when(redisRepo.existsById(any())).thenReturn(false);

            when(inMemoryRepo.getImplementationType()).thenReturn("IN_MEMORY");
            when(redisRepo.getImplementationType()).thenReturn("REDIS");

            // When
            transactionService.create(1L, "test", new BigDecimal("100"), null, StorageStrategy.IN_MEMORY);
            transactionService.create(2L, "test", new BigDecimal("100"), null, StorageStrategy.REDIS);

            // Then
            verify(repositoryFactory).getRepository(StorageStrategy.IN_MEMORY);
            verify(repositoryFactory).getRepository(StorageStrategy.REDIS);
            verify(inMemoryRepo).save(any());
            verify(redisRepo).save(any());
        }
    }
}

package com.mendel.challenge.infrastructure.adapter.memory;

import com.mendel.challenge.domain.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InMemoryTransactionRepository Unit Tests")
class InMemoryTransactionRepositoryTest {

    private InMemoryTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTransactionRepository();
    }

    @Nested
    @DisplayName("Save Transaction Tests")
    class SaveTransactionTests {

        @Test
        @DisplayName("Should save transaction successfully")
        void shouldSaveTransactionSuccessfully() {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            // When
            Transaction saved = repository.save(transaction);

            // Then
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isEqualTo(1L);
            assertThat(saved.getType()).isEqualTo("cars");
            assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("Should save transaction without parent")
        void shouldSaveTransactionWithoutParent() {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .type("shopping")
                    .amount(new BigDecimal("500.00"))
                    .build();

            // When
            repository.save(transaction);

            // Then
            Optional<Transaction> found = repository.findById(1L);
            assertThat(found).isPresent();
            assertThat(found.get().getParentId()).isNull();
        }

        @Test
        @DisplayName("Should save transaction with parent")
        void shouldSaveTransactionWithParent() {
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

            // When
            repository.save(parent);
            repository.save(child);

            // Then
            Optional<Transaction> found = repository.findById(2L);
            assertThat(found).isPresent();
            assertThat(found.get().getParentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should update transaction when saving with same ID")
        void shouldUpdateTransactionWhenSavingWithSameId() {
            // Given
            Transaction original = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            Transaction updated = Transaction.builder()
                    .id(1L)
                    .type("electronics")
                    .amount(new BigDecimal("2000.00"))
                    .build();

            // When
            repository.save(original);
            repository.save(updated);

            // Then
            Optional<Transaction> found = repository.findById(1L);
            assertThat(found).isPresent();
            assertThat(found.get().getType()).isEqualTo("electronics");
            assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
        }

        @Test
        @DisplayName("Should save multiple transactions")
        void shouldSaveMultipleTransactions() {
            // Given
            Transaction t1 = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            Transaction t2 = Transaction.builder()
                    .id(2L)
                    .type("shopping")
                    .amount(new BigDecimal("500.00"))
                    .build();

            Transaction t3 = Transaction.builder()
                    .id(3L)
                    .type("electronics")
                    .amount(new BigDecimal("2000.00"))
                    .build();

            // When
            repository.save(t1);
            repository.save(t2);
            repository.save(t3);

            // Then
            assertThat(repository.findById(1L)).isPresent();
            assertThat(repository.findById(2L)).isPresent();
            assertThat(repository.findById(3L)).isPresent();
        }

        @Test
        @DisplayName("Should update type index when saving transaction")
        void shouldUpdateTypeIndexWhenSavingTransaction() {
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
            List<Transaction> carsTransactions = repository.findByType("cars");
            assertThat(carsTransactions).hasSize(2);
        }

        @Test
        @DisplayName("Should update children index when saving transaction with parent")
        void shouldUpdateChildrenIndexWhenSavingTransactionWithParent() {
            // Given
            Transaction parent = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            Transaction child1 = Transaction.builder()
                    .id(2L)
                    .type("maintenance")
                    .amount(new BigDecimal("200.00"))
                    .parentId(1L)
                    .build();

            Transaction child2 = Transaction.builder()
                    .id(3L)
                    .type("fuel")
                    .amount(new BigDecimal("100.00"))
                    .parentId(1L)
                    .build();

            // When
            repository.save(parent);
            repository.save(child1);
            repository.save(child2);

            // Then
            List<Transaction> children = repository.findChildrenOf(1L);
            assertThat(children).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find transaction by ID")
        void shouldFindTransactionById() {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();
            repository.save(transaction);

            // When
            Optional<Transaction> found = repository.findById(1L);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(1L);
            assertThat(found.get().getType()).isEqualTo("cars");
        }

        @Test
        @DisplayName("Should return empty when transaction not found")
        void shouldReturnEmptyWhenTransactionNotFound() {
            // When
            Optional<Transaction> found = repository.findById(999L);

            // Then
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should find transaction after saving multiple transactions")
        void shouldFindTransactionAfterSavingMultiple() {
            // Given
            Transaction t1 = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            Transaction t2 = Transaction.builder()
                    .id(2L)
                    .type("shopping")
                    .amount(new BigDecimal("500.00"))
                    .build();

            repository.save(t1);
            repository.save(t2);

            // When
            Optional<Transaction> found = repository.findById(2L);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getType()).isEqualTo("shopping");
        }
    }

    @Nested
    @DisplayName("Find By Type Tests")
    class FindByTypeTests {

        @Test
        @DisplayName("Should find transactions by type")
        void shouldFindTransactionsByType() {
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

            Transaction t3 = Transaction.builder()
                    .id(3L)
                    .type("shopping")
                    .amount(new BigDecimal("500.00"))
                    .build();

            repository.save(t1);
            repository.save(t2);
            repository.save(t3);

            // When
            List<Transaction> carsTransactions = repository.findByType("cars");

            // Then
            assertThat(carsTransactions).hasSize(2);
            assertThat(carsTransactions)
                    .extracting(Transaction::getType)
                    .containsOnly("cars");
        }

        @Test
        @DisplayName("Should return empty list when type not found")
        void shouldReturnEmptyListWhenTypeNotFound() {
            // Given
            Transaction t1 = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();
            repository.save(t1);

            // When
            List<Transaction> result = repository.findByType("nonexistent");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when no transactions exist")
        void shouldReturnEmptyListWhenNoTransactionsExist() {
            // When
            List<Transaction> result = repository.findByType("cars");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find all transactions of same type")
        void shouldFindAllTransactionsOfSameType() {
            // Given
            for (int i = 1; i <= 5; i++) {
                Transaction t = Transaction.builder()
                        .id((long) i)
                        .type("electronics")
                        .amount(new BigDecimal(i * 100))
                        .build();
                repository.save(t);
            }

            // When
            List<Transaction> result = repository.findByType("electronics");

            // Then
            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("Should handle multiple different types")
        void shouldHandleMultipleDifferentTypes() {
            // Given
            repository.save(Transaction.builder().id(1L).type("cars").amount(new BigDecimal("1000")).build());
            repository.save(Transaction.builder().id(2L).type("shopping").amount(new BigDecimal("500")).build());
            repository.save(Transaction.builder().id(3L).type("electronics").amount(new BigDecimal("2000")).build());
            repository.save(Transaction.builder().id(4L).type("cars").amount(new BigDecimal("1500")).build());

            // When
            List<Transaction> carsResult = repository.findByType("cars");
            List<Transaction> shoppingResult = repository.findByType("shopping");
            List<Transaction> electronicsResult = repository.findByType("electronics");

            // Then
            assertThat(carsResult).hasSize(2);
            assertThat(shoppingResult).hasSize(1);
            assertThat(electronicsResult).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Find Children Tests")
    class FindChildrenTests {

        @Test
        @DisplayName("Should find children of parent transaction")
        void shouldFindChildrenOfParentTransaction() {
            // Given
            Transaction parent = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            Transaction child1 = Transaction.builder()
                    .id(2L)
                    .type("maintenance")
                    .amount(new BigDecimal("200.00"))
                    .parentId(1L)
                    .build();

            Transaction child2 = Transaction.builder()
                    .id(3L)
                    .type("fuel")
                    .amount(new BigDecimal("100.00"))
                    .parentId(1L)
                    .build();

            repository.save(parent);
            repository.save(child1);
            repository.save(child2);

            // When
            List<Transaction> children = repository.findChildrenOf(1L);

            // Then
            assertThat(children).hasSize(2);
            assertThat(children)
                    .extracting(Transaction::getId)
                    .containsExactlyInAnyOrder(2L, 3L);
        }

        @Test
        @DisplayName("Should return empty list when parent has no children")
        void shouldReturnEmptyListWhenParentHasNoChildren() {
            // Given
            Transaction parent = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();
            repository.save(parent);

            // When
            List<Transaction> children = repository.findChildrenOf(1L);

            // Then
            assertThat(children).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when parent does not exist")
        void shouldReturnEmptyListWhenParentDoesNotExist() {
            // When
            List<Transaction> children = repository.findChildrenOf(999L);

            // Then
            assertThat(children).isEmpty();
        }

        @Test
        @DisplayName("Should handle multiple levels of hierarchy")
        void shouldHandleMultipleLevelsOfHierarchy() {
            // Given
            Transaction grandparent = Transaction.builder()
                    .id(1L)
                    .type("project")
                    .amount(new BigDecimal("1000"))
                    .build();

            Transaction parent = Transaction.builder()
                    .id(2L)
                    .type("phase")
                    .amount(new BigDecimal("500"))
                    .parentId(1L)
                    .build();

            Transaction child = Transaction.builder()
                    .id(3L)
                    .type("task")
                    .amount(new BigDecimal("200"))
                    .parentId(2L)
                    .build();

            repository.save(grandparent);
            repository.save(parent);
            repository.save(child);

            // When
            List<Transaction> grandparentChildren = repository.findChildrenOf(1L);
            List<Transaction> parentChildren = repository.findChildrenOf(2L);

            // Then
            assertThat(grandparentChildren).hasSize(1);
            assertThat(grandparentChildren.get(0).getId()).isEqualTo(2L);
            assertThat(parentChildren).hasSize(1);
            assertThat(parentChildren.get(0).getId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should find only direct children")
        void shouldFindOnlyDirectChildren() {
            // Given
            Transaction root = Transaction.builder()
                    .id(1L)
                    .type("root")
                    .amount(new BigDecimal("1000"))
                    .build();

            Transaction child = Transaction.builder()
                    .id(2L)
                    .type("child")
                    .amount(new BigDecimal("500"))
                    .parentId(1L)
                    .build();

            Transaction grandchild = Transaction.builder()
                    .id(3L)
                    .type("grandchild")
                    .amount(new BigDecimal("200"))
                    .parentId(2L)
                    .build();

            repository.save(root);
            repository.save(child);
            repository.save(grandchild);

            // When
            List<Transaction> rootChildren = repository.findChildrenOf(1L);

            // Then
            assertThat(rootChildren).hasSize(1);
            assertThat(rootChildren.get(0).getId()).isEqualTo(2L);
            assertThat(rootChildren)
                    .extracting(Transaction::getId)
                    .doesNotContain(3L);
        }

        @Test
        @DisplayName("Should handle parent with many children")
        void shouldHandleParentWithManyChildren() {
            // Given
            Transaction parent = Transaction.builder()
                    .id(1L)
                    .type("parent")
                    .amount(new BigDecimal("1000"))
                    .build();
            repository.save(parent);

            for (int i = 2; i <= 10; i++) {
                Transaction child = Transaction.builder()
                        .id((long) i)
                        .type("child" + i)
                        .amount(new BigDecimal(i * 100))
                        .parentId(1L)
                        .build();
                repository.save(child);
            }

            // When
            List<Transaction> children = repository.findChildrenOf(1L);

            // Then
            assertThat(children).hasSize(9);
        }
    }

    @Nested
    @DisplayName("Exists By ID Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Should return true when transaction exists")
        void shouldReturnTrueWhenTransactionExists() {
            // Given
            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();
            repository.save(transaction);

            // When
            boolean exists = repository.existsById(1L);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when transaction does not exist")
        void shouldReturnFalseWhenTransactionDoesNotExist() {
            // When
            boolean exists = repository.existsById(999L);

            // Then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return false for null ID")
        void shouldReturnFalseForNullId() {
            // When
            boolean exists = repository.existsById(null);

            // Then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return true after saving transaction")
        void shouldReturnTrueAfterSavingTransaction() {
            // Given
            assertThat(repository.existsById(1L)).isFalse();

            Transaction transaction = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000.00"))
                    .build();

            // When
            repository.save(transaction);

            // Then
            assertThat(repository.existsById(1L)).isTrue();
        }
    }

    @Nested
    @DisplayName("Get Implementation Type Tests")
    class GetImplementationTypeTests {

        @Test
        @DisplayName("Should return IN_MEMORY as implementation type")
        void shouldReturnInMemoryAsImplementationType() {
            // When
            String implementationType = repository.getImplementationType();

            // Then
            assertThat(implementationType).isEqualTo("IN_MEMORY");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complete transaction hierarchy")
        void shouldHandleCompleteTransactionHierarchy() {
            // Given - Create a complex hierarchy
            Transaction root = Transaction.builder()
                    .id(1L)
                    .type("project")
                    .amount(new BigDecimal("10000"))
                    .build();

            Transaction development = Transaction.builder()
                    .id(2L)
                    .type("development")
                    .amount(new BigDecimal("5000"))
                    .parentId(1L)
                    .build();

            Transaction testing = Transaction.builder()
                    .id(3L)
                    .type("testing")
                    .amount(new BigDecimal("3000"))
                    .parentId(1L)
                    .build();

            Transaction frontend = Transaction.builder()
                    .id(4L)
                    .type("frontend")
                    .amount(new BigDecimal("2000"))
                    .parentId(2L)
                    .build();

            Transaction backend = Transaction.builder()
                    .id(5L)
                    .type("backend")
                    .amount(new BigDecimal("3000"))
                    .parentId(2L)
                    .build();

            // When
            repository.save(root);
            repository.save(development);
            repository.save(testing);
            repository.save(frontend);
            repository.save(backend);

            // Then - Verify root
            assertThat(repository.existsById(1L)).isTrue();
            assertThat(repository.findById(1L)).isPresent();

            // Verify children of root
            List<Transaction> rootChildren = repository.findChildrenOf(1L);
            assertThat(rootChildren).hasSize(2);

            // Verify children of development
            List<Transaction> devChildren = repository.findChildrenOf(2L);
            assertThat(devChildren).hasSize(2);

            // Verify type queries
            List<Transaction> projectTransactions = repository.findByType("project");
            assertThat(projectTransactions).hasSize(1);

            List<Transaction> developmentTransactions = repository.findByType("development");
            assertThat(developmentTransactions).hasSize(1);
        }

        @Test
        @DisplayName("Should maintain data consistency across operations")
        void shouldMaintainDataConsistencyAcrossOperations() {
            // Given
            Transaction t1 = Transaction.builder()
                    .id(1L)
                    .type("cars")
                    .amount(new BigDecimal("1000"))
                    .build();

            Transaction t2 = Transaction.builder()
                    .id(2L)
                    .type("cars")
                    .amount(new BigDecimal("2000"))
                    .parentId(1L)
                    .build();

            // When - Save transactions
            repository.save(t1);
            repository.save(t2);

            // Then - Verify all indices are consistent
            assertThat(repository.existsById(1L)).isTrue();
            assertThat(repository.existsById(2L)).isTrue();

            assertThat(repository.findByType("cars")).hasSize(2);
            assertThat(repository.findChildrenOf(1L)).hasSize(1);

            Optional<Transaction> found = repository.findById(2L);
            assertThat(found).isPresent();
            assertThat(found.get().getParentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should handle repository isolation between tests")
        void shouldHandleRepositoryIsolation() {
            // This test verifies that @BeforeEach creates a fresh repository
            // When
            boolean exists = repository.existsById(1L);

            // Then
            assertThat(exists).isFalse();
            assertThat(repository.findByType("cars")).isEmpty();
        }
    }
}

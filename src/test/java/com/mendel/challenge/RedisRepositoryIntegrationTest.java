package com.mendel.challenge;

import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Redis Repository Integration Test")
class RedisRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TransactionRepository repository;

    @Test
    @DisplayName("Should save and retrieve transaction from Redis")
    void shouldSaveAndRetrieveTransactionFromRedis() {
        Transaction transaction = Transaction.builder()
                .id(1L)
                .type("cars")
                .amount(new BigDecimal("5000.00"))
                .build();

        repository.save(transaction);
        Optional<Transaction> found = repository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo("cars");
    }

    @Test
    @DisplayName("Should find transactions by type")
    void shouldFindTransactionsByType() {
        // Given
        Transaction t1 = Transaction.builder()
                .id(10L)
                .type("electronics")
                .amount(new BigDecimal("1000.00"))
                .build();

        Transaction t2 = Transaction.builder()
                .id(11L)
                .type("electronics")
                .amount(new BigDecimal("2000.00"))
                .build();

        repository.save(t1);
        repository.save(t2);

        // When
        List<Transaction> found = repository.findByType("electronics");

        // Then
        assertThat(found).hasSize(2);
        assertThat(found)
                .extracting(Transaction::getId)
                .containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    @DisplayName("Should find children of parent transaction")
    void shouldFindChildrenOfParentTransaction() {
        // Given
        Transaction parent = Transaction.builder()
                .id(20L)
                .type("project")
                .amount(new BigDecimal("1000.00"))
                .build();

        Transaction child1 = Transaction.builder()
                .id(21L)
                .type("task")
                .amount(new BigDecimal("500.00"))
                .parentId(20L)
                .build();

        Transaction child2 = Transaction.builder()
                .id(22L)
                .type("task")
                .amount(new BigDecimal("300.00"))
                .parentId(20L)
                .build();

        repository.save(parent);
        repository.save(child1);
        repository.save(child2);

        // When
        List<Transaction> children = repository.findChildrenOf(20L);

        // Then
        assertThat(children).hasSize(2);
        assertThat(children)
                .extracting(Transaction::getParentId)
                .containsOnly(20L);
    }

    @Test
    @DisplayName("Should verify transaction exists")
    void shouldVerifyTransactionExists() {
        // Given
        Transaction transaction = Transaction.builder()
                .id(30L)
                .type("test")
                .amount(new BigDecimal("100.00"))
                .build();

        repository.save(transaction);

        // When
        boolean exists = repository.existsById(30L);
        boolean notExists = repository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }


    @Test
    @DisplayName("Should correctly map child-parent relationships in Redis")
    void shouldHandleComplexHierarchy() {
        // Parent
        Transaction p = Transaction.builder().id(100L).type("A").amount(BigDecimal.TEN).build();
        // Child
        Transaction c = Transaction.builder().id(101L).type("B").amount(BigDecimal.ONE).parentId(100L).build();

        repository.save(p);
        repository.save(c);

        List<Transaction> children = repository.findChildrenOf(100L);
        assertThat(children).hasSize(1);
        assertThat(children.get(0).getId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("Should return empty list for type with no transactions")
    void shouldReturnEmptyListForUnknownType() {
        List<Transaction> result = repository.findByType("unknown_type");
        assertThat(result).isEmpty();
    }

}


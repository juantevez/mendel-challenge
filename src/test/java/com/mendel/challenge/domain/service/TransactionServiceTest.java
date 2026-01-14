package com.mendel.challenge.domain.service;

import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(repository);
    }

    @Test
    @DisplayName("Should create transaction successfully when data is valid")
    void create_Success() {
        // Arrange
        Long id = 10L;
        Transaction transaction = Transaction.builder()
                .id(id)
                .type("cars")
                .amount(new BigDecimal("5000"))
                .build();

        when(repository.existsById(id)).thenReturn(false);
        when(repository.save(any(Transaction.class))).thenReturn(transaction);

        // Act
        Transaction result = transactionService.create(id, "cars", new BigDecimal("5000"), null);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(repository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when creating a transaction with existing ID")
    void create_DuplicateId_ThrowsException() {
        when(repository.existsById(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.create(1L, "cars", BigDecimal.TEN, null)
        );
    }

    @Test
    @DisplayName("Should calculate sum correctly for a transaction with multiple children levels")
    void calculateSum_RecursiveSuccess() {
        // Definimos un tipo genérico para evitar el NPE
        String type = "cars";

    /*
     Estructura:
     T1 (100)
      ├── T2 (50)
      └── T3 (30)
           └── T4 (20)
     Total esperado: 200
    */
        Transaction t1 = Transaction.builder().id(1L).type(type).amount(new BigDecimal("100")).build();
        Transaction t2 = Transaction.builder().id(2L).type(type).amount(new BigDecimal("50")).parentId(1L).build();
        Transaction t3 = Transaction.builder().id(3L).type(type).amount(new BigDecimal("30")).parentId(1L).build();
        Transaction t4 = Transaction.builder().id(4L).type(type).amount(new BigDecimal("20")).parentId(3L).build();

        when(repository.findById(1L)).thenReturn(Optional.of(t1));

        // Mock de hijos
        when(repository.findChildrenOf(1L)).thenReturn(List.of(t2, t3));
        when(repository.findChildrenOf(2L)).thenReturn(Collections.emptyList());
        when(repository.findChildrenOf(3L)).thenReturn(List.of(t4));
        when(repository.findChildrenOf(4L)).thenReturn(Collections.emptyList());

        // Act
        BigDecimal totalSum = transactionService.calculateSum(1L);

        // Assert
        // Es recomendable usar compareTo con BigDecimal para evitar problemas de escala (.00)
        assertTrue(new BigDecimal("200").compareTo(totalSum) == 0);
    }

    @Test
    @DisplayName("Should throw exception when calculating sum for non-existent transaction")
    void calculateSum_NotFound_ThrowsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.calculateSum(99L)
        );
    }
}

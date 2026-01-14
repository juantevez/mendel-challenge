package com.mendel.challenge.domain.service;

import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.in.CreateTransactionUseCase;
import com.mendel.challenge.domain.port.in.GetTransactionSumUseCase;
import com.mendel.challenge.domain.port.in.GetTransactionsByTypeUseCase;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class TransactionService implements
        CreateTransactionUseCase,
        GetTransactionsByTypeUseCase,
        GetTransactionSumUseCase {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Transaction create(Long id, String type, BigDecimal amount, Long parentId) {
        log.info("Starting transaction creation - id: {}, type: {}, amount: {}, parentId: {}",
                id, type, amount, parentId);

        if (repository.existsById(id)) {
            log.warn("Transaction creation failed - Transaction with id {} already exists", id);
            throw new IllegalArgumentException("Transaction already exists");
        }

        // Validación de existencia de padre
        if (parentId != null) {
            log.debug("Validating parent transaction with id: {}", parentId);

            if (!repository.existsById(parentId)) {
                log.warn("Transaction creation failed - Parent transaction with id {} not found", parentId);
                throw new IllegalArgumentException("Parent transaction not found");
            }

            log.debug("Parent transaction validated successfully");
        }

        Transaction transaction = Transaction.builder()
                .id(id)
                .type(type)
                .amount(amount)
                .parentId(parentId)
                .build();

        try {
            Transaction savedTransaction = repository.save(transaction);
            log.info("Transaction created successfully - id: {}, type: {}", id, type);
            return savedTransaction;

        } catch (Exception e) {
            log.error("Error saving transaction with id: {}. Error: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public BigDecimal calculateSum(Long transactionId) {
        log.info("Starting sum calculation for transaction id: {}", transactionId);

        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> {
                    log.warn("Sum calculation failed - Transaction with id {} not found", transactionId);
                    return new IllegalArgumentException("Transaction not found");
                });

        try {
            BigDecimal sum = calculateSumRecursive(transaction);
            log.info("Sum calculation completed for transaction id: {}. Total: {}", transactionId, sum);
            return sum;

        } catch (Exception e) {
            log.error("Error calculating sum for transaction id: {}. Error: {}", transactionId, e.getMessage(), e);
            throw e;
        }
    }

    private BigDecimal calculateSumRecursive(Transaction transaction) {
        log.debug("Calculating sum for transaction id: {}, amount: {}",
                transaction.getId(), transaction.getAmount());

        BigDecimal sum = transaction.getAmount();
        List<Transaction> children = repository.findChildrenOf(transaction.getId());

        if (!children.isEmpty()) {
            log.debug("Transaction id {} has {} children", transaction.getId(), children.size());
        }

        for (Transaction child : children) {
            BigDecimal childSum = calculateSumRecursive(child);
            sum = sum.add(childSum);
        }

        log.debug("Total sum for transaction id {}: {}", transaction.getId(), sum);
        return sum;
    }

    @Override
    public List<Transaction> getByType(String type) {
        log.info("Fetching transactions by type: {}", type);

        try {
            List<Transaction> transactions = repository.findByType(type);
            log.info("Found {} transactions of type: {}", transactions.size(), type);
            return transactions;

        } catch (Exception e) {
            log.error("Error fetching transactions by type: {}. Error: {}", type, e.getMessage(), e);
            throw e;
        }
    }
}

package com.mendel.challenge.domain.service;

import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.in.CreateTransactionUseCase;
import com.mendel.challenge.domain.port.in.GetTransactionSumUseCase;
import com.mendel.challenge.domain.port.in.GetTransactionsByTypeUseCase;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
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
        if (repository.existsById(id)) {
            throw new IllegalArgumentException("Transaction already exists");
        }

        // Validación de existencia de padre
        if (parentId != null && !repository.existsById(parentId)) {
            throw new IllegalArgumentException("Parent transaction not found");
        }

        Transaction transaction = Transaction.builder()
                .id(id)
                .type(type)
                .amount(amount)
                .parentId(parentId)
                .build();

        return repository.save(transaction);
    }

    @Override
    public BigDecimal calculateSum(Long transactionId) {
        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        return calculateSumRecursive(transaction);
    }

    private BigDecimal calculateSumRecursive(Transaction transaction) {
        BigDecimal sum = transaction.getAmount();
        List<Transaction> children = repository.findChildrenOf(transaction.getId());

        for (Transaction child : children) {
            sum = sum.add(calculateSumRecursive(child));
        }
        return sum;
    }

    @Override
    public List<Transaction> getByType(String type) {
        return repository.findByType(type);
    }
}

package com.mendel.challenge.domain.service;

import com.mendel.challenge.domain.model.StorageStrategy;
import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.in.CreateTransactionUseCase;
import com.mendel.challenge.domain.port.in.GetTransactionSumUseCase;
import com.mendel.challenge.domain.port.in.GetTransactionsByTypeUseCase;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import com.mendel.challenge.infrastructure.factory.TransactionRepositoryFactory;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService implements
        CreateTransactionUseCase,
        GetTransactionsByTypeUseCase,
        GetTransactionSumUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepositoryFactory repositoryFactory;

    public TransactionService(TransactionRepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    public Transaction create(Long id, String type, BigDecimal amount, Long parentId, StorageStrategy strategy) {
        TransactionRepository repository = repositoryFactory.getRepository(strategy);
        log.info("Creating transaction with {} storage", repository.getImplementationType());

        if (repository.existsById(id)) {
            throw new IllegalArgumentException("Transaction with id " + id + " already exists");
        }

        if (parentId != null && !repository.existsById(parentId)) {
            throw new IllegalArgumentException("Parent transaction " + parentId + " does not exist");
        }

        Transaction transaction = Transaction.builder()
                .id(id)
                .type(type)
                .amount(amount)
                .parentId(parentId)
                .build();

        return repository.save(transaction);
    }

    public List<Transaction> getByType(String type, StorageStrategy strategy) {
        TransactionRepository repository = repositoryFactory.getRepository(strategy);
        log.info("Getting transactions by type with {} storage", repository.getImplementationType());
        return repository.findByType(type);
    }

    public BigDecimal calculateSum(Long transactionId, StorageStrategy strategy) {
        TransactionRepository repository = repositoryFactory.getRepository(strategy);
        log.info("Calculating sum with {} storage", repository.getImplementationType());

        Transaction transaction = repository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction " + transactionId + " not found"));

        return calculateSumRecursive(transaction, repository);
    }

    private BigDecimal calculateSumRecursive(Transaction transaction, TransactionRepository repository) {
        BigDecimal sum = transaction.getAmount();

        List<Transaction> children = repository.findChildrenOf(transaction.getId());

        for (Transaction child : children) {
            sum = sum.add(calculateSumRecursive(child, repository));
        }

        return sum;
    }

    // Métodos de interfaz delegando a versiones con strategy
    @Override
    public Transaction create(Long id, String type, BigDecimal amount, Long parentId) {
        return create(id, type, amount, parentId, StorageStrategy.IN_MEMORY);
    }

    @Override
    public List<Transaction> getByType(String type) {
        return getByType(type, StorageStrategy.IN_MEMORY);
    }

    @Override
    public BigDecimal calculateSum(Long transactionId) {
        return calculateSum(transactionId, StorageStrategy.IN_MEMORY);
    }
}

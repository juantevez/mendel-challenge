package com.mendel.challenge.domain.port.out;

import com.mendel.challenge.domain.model.Transaction;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(Long id);
    List<Transaction> findByType(String type);
    List<Transaction> findChildrenOf(Long parentId);
    boolean existsById(Long id);

    String getImplementationType();
}

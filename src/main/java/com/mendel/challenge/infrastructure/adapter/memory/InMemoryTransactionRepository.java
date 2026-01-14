package com.mendel.challenge.infrastructure.adapter.memory;

import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<Long, Transaction> transactions = new HashMap<>();
    private final Map<String, Set<Long>> typeIndex = new HashMap<>();
    private final Map<Long, Set<Long>> childrenIndex = new HashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        transactions.put(transaction.getId(), transaction);

        typeIndex.computeIfAbsent(transaction.getType(), k -> new HashSet<>())
                .add(transaction.getId());

        if (transaction.hasParent()) {
            childrenIndex.computeIfAbsent(transaction.getParentId(), k -> new HashSet<>())
                    .add(transaction.getId());
        }

        return transaction;
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return Optional.ofNullable(transactions.get(id));
    }

    @Override
    public List<Transaction> findByType(String type) {
        Set<Long> ids = typeIndex.getOrDefault(type, Collections.emptySet());

        return ids.stream()
                .map(transactions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findChildrenOf(Long parentId) {
        Set<Long> childIds = childrenIndex.getOrDefault(parentId, Collections.emptySet());

        return childIds.stream()
                .map(transactions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Long id) {
        return transactions.containsKey(id);
    }

    public String getImplementationType() {
        return "IN_MEMORY";
    }
}

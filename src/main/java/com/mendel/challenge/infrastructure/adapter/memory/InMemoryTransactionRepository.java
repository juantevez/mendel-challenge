package com.mendel.challenge.infrastructure.adapter.memory;

import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<Long, Transaction> transactions = new HashMap<>();
    private final Map<String, Set<Long>> typeIndex = new HashMap<>();
    private final Map<Long, Set<Long>> childrenIndex = new HashMap<>();

    @Override
    public Transaction save(Transaction transaction) {
        log.debug("Saving transaction - id: {}, type: {}, amount: {}, parentId: {}",
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getParentId());

        transactions.put(transaction.getId(), transaction);

        typeIndex.computeIfAbsent(transaction.getType(), k -> {
            log.debug("Creating new type index entry for type: {}", k);
            return new HashSet<>();
        }).add(transaction.getId());

        if (transaction.hasParent()) {
            log.debug("Indexing transaction {} as child of parent: {}",
                    transaction.getId(), transaction.getParentId());

            childrenIndex.computeIfAbsent(transaction.getParentId(), k -> {
                log.debug("Creating new children index entry for parent: {}", k);
                return new HashSet<>();
            }).add(transaction.getId());
        }

        log.info("Transaction saved successfully - id: {}, total transactions: {}",
                transaction.getId(), transactions.size());

        return transaction;
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        log.debug("Searching transaction by id: {}", id);

        Optional<Transaction> result = Optional.ofNullable(transactions.get(id));

        if (result.isPresent()) {
            log.debug("Transaction found with id: {}", id);
        } else {
            log.debug("Transaction not found with id: {}", id);
        }

        return result;
    }

    @Override
    public List<Transaction> findByType(String type) {
        log.debug("Searching transactions by type: {}", type);

        Set<Long> ids = typeIndex.getOrDefault(type, Collections.emptySet());

        log.debug("Found {} transaction ids for type: {}", ids.size(), type);

        List<Transaction> result = ids.stream()
                .map(transactions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Retrieved {} transactions of type: {}", result.size(), type);

        return result;
    }

    @Override
    public List<Transaction> findChildrenOf(Long parentId) {
        log.debug("Searching children transactions of parent id: {}", parentId);

        Set<Long> childIds = childrenIndex.getOrDefault(parentId, Collections.emptySet());

        log.debug("Found {} child ids for parent: {}", childIds.size(), parentId);

        List<Transaction> result = childIds.stream()
                .map(transactions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Retrieved {} children transactions for parent id: {}", result.size(), parentId);

        return result;
    }

    @Override
    public boolean existsById(Long id) {
        boolean exists = transactions.containsKey(id);

        log.debug("Checking existence of transaction id: {} - exists: {}", id, exists);

        return exists;
    }

    public String getImplementationType() {
        log.debug("Getting implementation type: IN_MEMORY");
        return "IN_MEMORY";
    }

    // Métodos adicionales útiles para monitoring
    public int getTransactionCount() {
        int count = transactions.size();
        log.debug("Current transaction count: {}", count);
        return count;
    }

    public int getTypeIndexSize() {
        int size = typeIndex.size();
        log.debug("Current type index size: {}", size);
        return size;
    }

    public int getChildrenIndexSize() {
        int size = childrenIndex.size();
        log.debug("Current children index size: {}", size);
        return size;
    }
}

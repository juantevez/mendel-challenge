package com.mendel.challenge.infrastructure.adapter.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository("redisRepository")
public class RedisTransactionRepository implements TransactionRepository {

    private static final String TRANSACTION_KEY_PREFIX = "transaction:";
    private static final String TYPE_INDEX_PREFIX = "type:";
    private static final String CHILDREN_INDEX_PREFIX = "children:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTransactionRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Transaction save(Transaction transaction) {
        String key = getTransactionKey(transaction.getId());
        String json = serializeTransaction(transaction);

        // Guardar la transacción
        redisTemplate.opsForValue().set(key, json);

        // Actualizar índice por tipo
        String typeKey = getTypeKey(transaction.getType());
        redisTemplate.opsForSet().add(typeKey, transaction.getId().toString());

        // Actualizar índice de hijos si tiene parent
        if (transaction.hasParent()) {
            String childrenKey = getChildrenKey(transaction.getParentId());
            redisTemplate.opsForSet().add(childrenKey, transaction.getId().toString());
        }

        return transaction;
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        String key = getTransactionKey(id);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return Optional.empty();
        }

        return Optional.of(deserializeTransaction(json));
    }

    @Override
    public List<Transaction> findByType(String type) {
        String typeKey = getTypeKey(type);
        Set<String> ids = redisTemplate.opsForSet().members(typeKey);

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        return ids.stream()
                .map(Long::parseLong)
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findChildrenOf(Long parentId) {
        String childrenKey = getChildrenKey(parentId);
        Set<String> childIds = redisTemplate.opsForSet().members(childrenKey);

        if (childIds == null || childIds.isEmpty()) {
            return Collections.emptyList();
        }

        return childIds.stream()
                .map(Long::parseLong)
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Long id) {
        String key = getTransactionKey(id);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public String getImplementationType() {
        return "REDIS";
    }

    // Helper methods
    private String getTransactionKey(Long id) {
        return TRANSACTION_KEY_PREFIX + id;
    }

    private String getTypeKey(String type) {
        return TYPE_INDEX_PREFIX + type;
    }

    private String getChildrenKey(Long parentId) {
        return CHILDREN_INDEX_PREFIX + parentId;
    }

    private String serializeTransaction(Transaction transaction) {
        try {
            return objectMapper.writeValueAsString(transaction);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing transaction", e);
        }
    }

    private Transaction deserializeTransaction(String json) {
        try {
            return objectMapper.readValue(json, Transaction.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing transaction", e);
        }
    }


}

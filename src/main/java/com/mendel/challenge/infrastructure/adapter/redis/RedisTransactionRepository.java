package com.mendel.challenge.infrastructure.adapter.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import com.mendel.challenge.infrastructure.adapter.redis.dto.TransactionRedisDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisTransactionRepository implements TransactionRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisTransactionRepository.class);

    private static final String TRANSACTION_KEY_PREFIX = "transaction:";
    private static final String TYPE_INDEX_PREFIX = "type:";
    private static final String CHILDREN_INDEX_PREFIX = "children:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTransactionRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        log.info("Redis Transaction Repository initialized");
    }

    @Override
    public Transaction save(Transaction transaction) {
        String key = getTransactionKey(transaction.getId());
        String json = serializeTransaction(transaction);

        redisTemplate.opsForValue().set(key, json);

        String typeKey = getTypeKey(transaction.getType());
        redisTemplate.opsForSet().add(typeKey, transaction.getId().toString());

        if (transaction.hasParent()) {
            String childrenKey = getChildrenKey(transaction.getParentId());
            redisTemplate.opsForSet().add(childrenKey, transaction.getId().toString());
        }

        log.debug("Saved transaction {} to Redis", transaction.getId());
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        String key = getTransactionKey(id);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            log.debug("Transaction {} not found in Redis", id);
            return Optional.empty();
        }

        Transaction transaction = deserializeTransaction(json);
        log.debug("Found transaction {} in Redis", id);
        return Optional.of(transaction);
    }

    @Override
    public List<Transaction> findByType(String type) {
        String typeKey = getTypeKey(type);
        Set<String> ids = redisTemplate.opsForSet().members(typeKey);

        if (ids == null || ids.isEmpty()) {
            log.debug("No transactions found for type {} in Redis", type);
            return Collections.emptyList();
        }

        List<Transaction> transactions = ids.stream()
                .map(Long::parseLong)
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        log.debug("Found {} transactions for type {} in Redis", transactions.size(), type);
        return transactions;
    }

    @Override
    public List<Transaction> findChildrenOf(Long parentId) {
        String childrenKey = getChildrenKey(parentId);
        Set<String> childIds = redisTemplate.opsForSet().members(childrenKey);

        if (childIds == null || childIds.isEmpty()) {
            log.debug("No children found for parent {} in Redis", parentId);
            return Collections.emptyList();
        }

        List<Transaction> children = childIds.stream()
                .map(Long::parseLong)
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        log.debug("Found {} children for parent {} in Redis", children.size(), parentId);
        return children;
    }

    @Override
    public boolean existsById(Long id) {
        String key = getTransactionKey(id);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    public String getImplementationType() {
        return "REDIS";
    }

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
            TransactionRedisDTO dto = TransactionRedisDTO.fromDomain(transaction);
            String json = objectMapper.writeValueAsString(dto);
            log.trace("Serialized transaction {}: {}", transaction.getId(), json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Error serializing transaction {}", transaction.getId(), e);
            throw new RuntimeException("Error serializing transaction", e);
        }
    }

    private Transaction deserializeTransaction(String json) {
        try {
            log.trace("Deserializing transaction: {}", json);
            TransactionRedisDTO dto = objectMapper.readValue(json, TransactionRedisDTO.class);
            Transaction transaction = dto.toDomain();
            log.trace("Deserialized transaction {}", transaction.getId());
            return transaction;
        } catch (JsonProcessingException e) {
            log.error("Error deserializing transaction from JSON: {}", json, e);
            throw new RuntimeException("Error deserializing transaction", e);
        }
    }
}

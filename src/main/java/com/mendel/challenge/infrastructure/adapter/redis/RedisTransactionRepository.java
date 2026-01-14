package com.mendel.challenge.infrastructure.adapter.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import com.mendel.challenge.infrastructure.adapter.redis.dto.TransactionRedisDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisTransactionRepository implements TransactionRepository {

    private static final String TRANSACTION_KEY_PREFIX = "transaction:";
    private static final String TYPE_INDEX_PREFIX = "type:";
    private static final String CHILDREN_INDEX_PREFIX = "children:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisTransactionRepository(RedisTemplate<String, String> redisTemplate) {
        log.info("Initializing Redis Transaction Repository");
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        log.info("Redis Transaction Repository initialized successfully");
    }

    @Override
    public Transaction save(Transaction transaction) {
        log.debug("Starting transaction save - id: {}, type: {}, amount: {}, parentId: {}",
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getParentId());

        try {
            String key = getTransactionKey(transaction.getId());
            String json = serializeTransaction(transaction);

            redisTemplate.opsForValue().set(key, json);
            log.debug("Transaction data saved to Redis with key: {}", key);

            // Indexar por tipo
            String typeKey = getTypeKey(transaction.getType());
            redisTemplate.opsForSet().add(typeKey, transaction.getId().toString());
            log.debug("Transaction {} indexed by type: {}", transaction.getId(), transaction.getType());

            // Indexar por padre si existe
            if (transaction.hasParent()) {
                String childrenKey = getChildrenKey(transaction.getParentId());
                redisTemplate.opsForSet().add(childrenKey, transaction.getId().toString());
                log.debug("Transaction {} indexed as child of parent: {}",
                        transaction.getId(), transaction.getParentId());
            }

            log.info("Transaction saved successfully to Redis - id: {}, type: {}",
                    transaction.getId(), transaction.getType());
            return transaction;

        } catch (Exception e) {
            log.error("Error saving transaction {} to Redis. Error: {}",
                    transaction.getId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        log.debug("Searching transaction by id in Redis: {}", id);

        try {
            String key = getTransactionKey(id);
            String json = redisTemplate.opsForValue().get(key);

            if (json == null) {
                log.debug("Transaction not found in Redis - id: {}", id);
                return Optional.empty();
            }

            Transaction transaction = deserializeTransaction(json);
            log.debug("Transaction found in Redis - id: {}, type: {}", id, transaction.getType());
            return Optional.of(transaction);

        } catch (Exception e) {
            log.error("Error finding transaction {} in Redis. Error: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Transaction> findByType(String type) {
        log.debug("Searching transactions by type in Redis: {}", type);

        try {
            String typeKey = getTypeKey(type);
            Set<String> ids = redisTemplate.opsForSet().members(typeKey);

            if (ids == null || ids.isEmpty()) {
                log.debug("No transaction ids found for type in Redis: {}", type);
                return Collections.emptyList();
            }

            log.debug("Found {} transaction ids for type: {}", ids.size(), type);

            List<Transaction> transactions = ids.stream()
                    .map(Long::parseLong)
                    .map(this::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            log.info("Retrieved {} transactions of type {} from Redis", transactions.size(), type);
            return transactions;

        } catch (Exception e) {
            log.error("Error finding transactions by type {} in Redis. Error: {}",
                    type, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Transaction> findChildrenOf(Long parentId) {
        log.debug("Searching children transactions of parent {} in Redis", parentId);

        try {
            String childrenKey = getChildrenKey(parentId);
            Set<String> childIds = redisTemplate.opsForSet().members(childrenKey);

            if (childIds == null || childIds.isEmpty()) {
                log.debug("No children found for parent {} in Redis", parentId);
                return Collections.emptyList();
            }

            log.debug("Found {} child ids for parent: {}", childIds.size(), parentId);

            List<Transaction> children = childIds.stream()
                    .map(Long::parseLong)
                    .map(this::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            log.debug("Retrieved {} children transactions for parent {} from Redis",
                    children.size(), parentId);
            return children;

        } catch (Exception e) {
            log.error("Error finding children of parent {} in Redis. Error: {}",
                    parentId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean existsById(Long id) {
        log.debug("Checking existence of transaction {} in Redis", id);

        try {
            String key = getTransactionKey(id);
            Boolean exists = redisTemplate.hasKey(key);
            boolean result = Boolean.TRUE.equals(exists);

            log.debug("Transaction {} exists in Redis: {}", id, result);
            return result;

        } catch (Exception e) {
            log.error("Error checking existence of transaction {} in Redis. Error: {}",
                    id, e.getMessage(), e);
            throw e;
        }
    }

    public String getImplementationType() {
        log.debug("Getting implementation type: REDIS");
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

            if (log.isTraceEnabled()) {
                log.trace("Serialized transaction {} to JSON: {}", transaction.getId(), json);
            }

            return json;

        } catch (JsonProcessingException e) {
            log.error("Error serializing transaction {} to JSON. Error: {}",
                    transaction.getId(), e.getMessage(), e);
            throw new RuntimeException("Error serializing transaction", e);
        }
    }

    private Transaction deserializeTransaction(String json) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Deserializing transaction from JSON: {}", json);
            }

            TransactionRedisDTO dto = objectMapper.readValue(json, TransactionRedisDTO.class);
            Transaction transaction = dto.toDomain();

            log.trace("Successfully deserialized transaction with id: {}", transaction.getId());
            return transaction;

        } catch (JsonProcessingException e) {
            log.error("Error deserializing transaction from JSON. JSON: {}. Error: {}",
                    json, e.getMessage(), e);
            throw new RuntimeException("Error deserializing transaction", e);
        }
    }
}

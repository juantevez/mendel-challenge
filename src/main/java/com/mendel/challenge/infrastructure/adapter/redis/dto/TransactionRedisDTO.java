package com.mendel.challenge.infrastructure.adapter.redis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mendel.challenge.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionRedisDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("parentId")
    private Long parentId;

    @JsonProperty("createdAt")
    private Instant createdAt;

    // Constructor vacío para Jackson
    public TransactionRedisDTO() {
    }

    public TransactionRedisDTO(Long id, String type, BigDecimal amount, Long parentId, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.parentId = parentId;
        this.createdAt = createdAt;
    }

    // Factory method desde Transaction
    public static TransactionRedisDTO fromDomain(Transaction transaction) {
        return new TransactionRedisDTO(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getParentId(),
                transaction.getCreatedAt()
        );
    }

    // Método para convertir a Transaction
    public Transaction toDomain() {
        return Transaction.builder()
                .id(id)
                .type(type)
                .amount(amount)
                .parentId(parentId)
                .createdAt(createdAt)
                .build();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

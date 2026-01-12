package com.mendel.challenge.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class Transaction {
    private final Long id;
    private final String type;
    private final BigDecimal amount;
    private final Long parentId;
    private final Instant createdAt;

    private Transaction(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Id cannot be null");
        this.type = Objects.requireNonNull(builder.type, "Type cannot be null");
        this.amount = Objects.requireNonNull(builder.amount, "Amount cannot be null");
        this.parentId = builder.parentId;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();

        validateAmount();
    }

    private void validateAmount() {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long getParentId() {
        return parentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean hasParent() {
        return parentId != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String type;
        private BigDecimal amount;
        private Long parentId;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder parentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }
}

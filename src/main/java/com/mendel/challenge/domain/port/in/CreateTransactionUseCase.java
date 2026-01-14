package com.mendel.challenge.domain.port.in;

import com.mendel.challenge.domain.model.Transaction;

import java.math.BigDecimal;

public interface CreateTransactionUseCase {
    Transaction create(Long id, String type, BigDecimal amount, Long parentId);
}
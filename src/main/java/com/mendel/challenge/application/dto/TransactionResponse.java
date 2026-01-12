package com.mendel.challenge.application.dto;


import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        String type,
        BigDecimal amount,
        Long parentId,
        Instant createdAt
) {}

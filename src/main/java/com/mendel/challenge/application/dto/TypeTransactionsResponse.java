package com.mendel.challenge.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TypeTransactionsResponse(
        String type,
        List<Long> transactionIds,
        Integer count,
        String storage
) {
    public static TypeTransactionsResponse of(String type, List<Long> transactionIds, String storage) {
        return new TypeTransactionsResponse(type, transactionIds, transactionIds.size(), storage);
    }
}
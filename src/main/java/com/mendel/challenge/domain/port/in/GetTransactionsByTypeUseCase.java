package com.mendel.challenge.domain.port.in;


import com.mendel.challenge.domain.model.Transaction;

import java.util.List;

public interface GetTransactionsByTypeUseCase {
    List<Transaction> getByType(String type);
}
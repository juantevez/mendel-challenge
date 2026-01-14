package com.mendel.challenge.domain.port.in;


import java.math.BigDecimal;

public interface GetTransactionSumUseCase {
    BigDecimal calculateSum(Long transactionId);
}

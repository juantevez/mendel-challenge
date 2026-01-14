package com.mendel.challenge.application.rest;

import com.mendel.challenge.application.dto.SumResponse;
import com.mendel.challenge.application.dto.TransactionRequest;
import com.mendel.challenge.application.dto.TransactionResponse;
import com.mendel.challenge.application.dto.TypeTransactionsResponse;
import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.service.TransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactionservice")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PutMapping("/transaction/{transaction_id}")
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable("transaction_id") Long transactionId,
            @Valid @RequestBody TransactionRequest request) {

        log.info("Creating transaction with id: {}, type: {}, amount: {}, parentId: {}",
                transactionId, request.type(), request.amount(), request.parentId());

        try {
            Transaction transaction = transactionService.create(
                    transactionId,
                    request.type(),
                    request.amount(),
                    request.parentId()
            );

            log.info("Transaction created successfully with id: {}", transactionId);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(transaction));

        } catch (Exception e) {
            log.error("Error creating transaction with id: {}. Error: {}", transactionId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/sum/{transaction_id}")
    public ResponseEntity<SumResponse> getTransactionSum(
            @PathVariable("transaction_id") Long transactionId) {

        log.info("Calculating sum for transaction id: {}", transactionId);

        try {
            BigDecimal sum = transactionService.calculateSum(transactionId);
            log.info("Sum calculated successfully for transaction id: {}. Result: {}", transactionId, sum);
            return ResponseEntity.ok(new SumResponse(sum));

        } catch (Exception e) {
            log.error("Error calculating sum for transaction id: {}. Error: {}", transactionId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/types/{type}")
    public ResponseEntity<TypeTransactionsResponse> getTransactionsByType(
            @PathVariable String type) {

        log.info("Fetching transactions by type: {}", type);

        try {
            List<Long> transactionIds = transactionService.getByType(type)
                    .stream()
                    .map(Transaction::getId)
                    .toList();

            log.info("Found {} transactions of type: {}", transactionIds.size(), type);

            TypeTransactionsResponse response = TypeTransactionsResponse.of(
                    type,
                    transactionIds,
                    "MANAGED_STORAGE"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching transactions by type: {}. Error: {}", type, e.getMessage(), e);
            throw e;
        }
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getParentId(),
                transaction.getCreatedAt()
        );
    }
}

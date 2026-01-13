package com.mendel.challenge.application.rest;

import com.mendel.challenge.application.dto.SumResponse;
import com.mendel.challenge.application.dto.TransactionRequest;
import com.mendel.challenge.application.dto.TransactionResponse;
import com.mendel.challenge.application.dto.TypeTransactionsResponse;
import com.mendel.challenge.domain.model.StorageStrategy;
import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactionservice")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PutMapping("/transaction/{transaction_id}")
    public ResponseEntity<TransactionResponse> createTransaction(
            @PathVariable("transaction_id") Long transactionId,
            @Valid @RequestBody TransactionRequest request,
            @RequestParam(value = "storage", defaultValue = "memory") String storage) {

        StorageStrategy strategy = StorageStrategy.fromString(storage);

        Transaction transaction = transactionService.create(
                transactionId,
                request.type(),
                request.amount(),
                request.parentId(),
                strategy
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(transaction));
    }

    @GetMapping("/types/{type}")
    public ResponseEntity<TypeTransactionsResponse> getTransactionsByType(
            @PathVariable String type,
            @RequestParam(value = "storage", defaultValue = "memory") String storage) {

        StorageStrategy strategy = StorageStrategy.fromString(storage);

        List<Long> transactionIds = transactionService.getByType(type, strategy)
                .stream()
                .map(Transaction::getId)
                .toList();

        TypeTransactionsResponse response = TypeTransactionsResponse.of(
                type,
                transactionIds,
                strategy.getValue()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sum/{transaction_id}")
    public ResponseEntity<SumResponse> getTransactionSum(
            @PathVariable("transaction_id") Long transactionId,
            @RequestParam(value = "storage", defaultValue = "memory") String storage) {

        StorageStrategy strategy = StorageStrategy.fromString(storage);

        var sum = transactionService.calculateSum(transactionId, strategy);
        return ResponseEntity.ok(new SumResponse(sum));
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

package com.mendel.challenge.application.rest;

import com.mendel.challenge.application.dto.SumResponse;
import com.mendel.challenge.application.dto.TransactionRequest;
import com.mendel.challenge.application.dto.TransactionResponse;
import com.mendel.challenge.application.dto.TypeTransactionsResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
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
            @Valid @RequestBody TransactionRequest request) {

        Transaction transaction = transactionService.create(
                transactionId,
                request.type(),
                request.amount(),
                request.parentId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(transaction));
    }

    @GetMapping("/sum/{transaction_id}")
    public ResponseEntity<SumResponse> getTransactionSum(
            @PathVariable("transaction_id") Long transactionId) {

        BigDecimal sum = transactionService.calculateSum(transactionId);
        return ResponseEntity.ok(new SumResponse(sum));
    }

    @GetMapping("/types/{type}")
    public ResponseEntity<TypeTransactionsResponse> getTransactionsByType(
            @PathVariable String type) {

        List<Long> transactionIds = transactionService.getByType(type)
                .stream()
                .map(Transaction::getId)
                .toList();

        // Si tu DTO TypeTransactionsResponse.of todavía requiere 3 parámetros:
        TypeTransactionsResponse response = TypeTransactionsResponse.of(
                type,
                transactionIds,
                "MANAGED_STORAGE"
        );

        return ResponseEntity.ok(response);
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

package com.mendel.challenge;

import com.mendel.challenge.domain.model.Transaction;
import com.mendel.challenge.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Memory Repository Integration Test")
@SpringBootTest(properties = "storage.strategy=memory")
class MemoryRepositoryIntegrationTest {

    @Autowired
    private TransactionRepository repository;

    @Test
    void shouldSaveInMemory() {
        Transaction t = Transaction.builder().id(1L).type("test").amount(BigDecimal.TEN).build();
        repository.save(t);
        assertThat(repository.findById(1L)).isPresent();
    }
}

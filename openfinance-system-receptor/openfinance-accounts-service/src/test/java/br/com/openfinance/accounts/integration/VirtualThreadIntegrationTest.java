package br.com.openfinance.accounts.integration;

import br.com.openfinance.accounts.OpenfinanceAccountsServiceApplication;
import br.com.openfinance.accounts.application.services.VirtualThreadAccountBatchProcessor;
import br.com.openfinance.accounts.domain.model.AccountStatus;
import br.com.openfinance.accounts.domain.model.AccountSubType;
import br.com.openfinance.accounts.domain.model.AccountType;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity.AccountEntity;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.repository.AccountJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OpenfinanceAccountsServiceApplication.class,
        properties = {
                "spring.profiles.active=test",
                "openfinance.accounts.batch.size=50",
                "openfinance.accounts.batch.virtual-threads.max=100"
        }
)
@Testcontainers
class VirtualThreadIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("openfinance_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private VirtualThreadAccountBatchProcessor batchProcessor;

    @Autowired
    private AccountJpaRepository accountRepository;

    @Test
    void shouldProcessLargeNumberOfAccountsWithVirtualThreads() {
        // Given - Create 1000 test accounts
        IntStream.range(0, 1000)
                .forEach(i -> accountRepository.save(createTestAccountEntity(i)));

        // When - Process with virtual threads
        long startTime = System.currentTimeMillis();
        batchProcessor.processAccountUpdatesWithVirtualThreads();
        long endTime = System.currentTimeMillis();

        // Then
        long processingTime = endTime - startTime;
        System.out.println("Processed 1000 accounts in " + processingTime + " ms using Virtual Threads");

        // Verify all accounts were processed
        assertThat(accountRepository.count()).isEqualTo(1000);

        // Processing should be fast with virtual threads
        assertThat(processingTime).isLessThan(10000); // Should complete within 10 seconds
    }

    private AccountEntity createTestAccountEntity(int index) {
        return AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .externalAccountId("EXT-" + index)
                .customerId("12345678901")
                .participantId("PART-001")
                .brandId("BRAND-001")
                .number(String.format("%06d", index))
                .checkDigit("0")
                .type(AccountType.CONTA_DEPOSITO_A_VISTA)
                .subtype(AccountSubType.INDIVIDUAL)
                .currency("BRL")
                .availableAmount(BigDecimal.valueOf(1000 + index))
                .blockedAmount(BigDecimal.ZERO)
                .automaticallyInvestedAmount(BigDecimal.ZERO)
                .lastUpdateDateTime(LocalDateTime.now())
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
package br.com.openfinance.service.accounts.adapter.output.persistence;

import br.com.openfinance.accounts.adapter.output.persistence.repository.AccountJpaRepository;
import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.valueobject.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16:///testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AccountRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private AccountJpaRepository jpaRepository;

    private AccountRepositoryAdapter repository;

    @Test
    void shouldSaveAndFindAccount() {
        // Given
        repository = new AccountRepositoryAdapter(jpaRepository, new AccountPersistenceMapper());

        var account = Account.builder()
                .id(AccountId.generate())
                .accountId("ACC123")
                .consentId(UUID.randomUUID())
                .organizationId("ORG123")
                .customerId("CUST123")
                .identification(new AccountIdentification("001", "0001", "12345678", "9"))
                .type(AccountType.CONTA_DEPOSITO_A_VISTA)
                .status(AccountStatus.AVAILABLE)
                .build();

        // When
        var saved = repository.save(account);
        var found = repository.findByAccountId("ACC123");

        // Then
        assertThat(saved).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getAccountId()).isEqualTo("ACC123");
    }
}

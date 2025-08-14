package br.com.openfinance.service.accounts.domain.model;


import br.com.openfinance.domain.account.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AccountTest {

    @Test
    @DisplayName("Should create account with builder")
    void shouldCreateAccountWithBuilder() {
        // Given
        var accountId = AccountId.generate();
        var consentId = UUID.randomUUID();
        var identification = new AccountIdentification("001", "0001", "12345678", "9");

        // When
        var account = Account.builder()
                .id(accountId)
                .accountId("ACC123")
                .consentId(consentId)
                .organizationId("ORG123")
                .customerId("CUST123")
                .identification(identification)
                .type(AccountType.CONTA_DEPOSITO_A_VISTA)
                .subtype(AccountSubType.INDIVIDUAL)
                .currency(Currency.BRL)
                .status(AccountStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .build();

        // Then
        assertThat(account).isNotNull();
        assertThat(account.getId()).isEqualTo(accountId);
        assertThat(account.getAccountId()).isEqualTo("ACC123");
        assertThat(account.getConsentId()).isEqualTo(consentId);
        assertThat(account.getType()).isEqualTo(AccountType.CONTA_DEPOSITO_A_VISTA);
        assertThat(account.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should determine if account needs sync")
    void shouldDetermineIfAccountNeedsSync() {
        // Given
        var account = Account.builder()
                .id(AccountId.generate())
                .accountId("ACC123")
                .lastSyncAt(LocalDateTime.now().minusMinutes(20))
                .build();

        // When/Then
        assertThat(account.needsSync()).isTrue();
    }

    @Test
    @DisplayName("Should get current balance")
    void shouldGetCurrentBalance() {
        // Given
        var balance1 = AccountBalance.builder()
                .availableAmount(new BigDecimal("1000.00"))
                .updateDateTime(LocalDateTime.now().minusHours(2))
                .build();

        var balance2 = AccountBalance.builder()
                .availableAmount(new BigDecimal("1500.00"))
                .updateDateTime(LocalDateTime.now())
                .build();

        var account = Account.builder()
                .id(AccountId.generate())
                .balances(List.of(balance1, balance2))
                .build();

        // When
        var currentBalance = account.getCurrentBalance();

        // Then
        assertThat(currentBalance).isNotNull();
        assertThat(currentBalance.getAvailableAmount()).isEqualTo(new BigDecimal("1500.00"));
    }
}

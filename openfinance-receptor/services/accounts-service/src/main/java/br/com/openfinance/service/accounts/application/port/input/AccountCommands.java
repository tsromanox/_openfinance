package br.com.openfinance.service.accounts.application.port.input;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Commands para operações de Account
 */
public class AccountCommands {

    public record SyncAccountsCommand(
            UUID consentId,
            String organizationId,
            String accessToken
    ) {}

    public record UpdateBalanceCommand(
            String accountId,
            BigDecimal availableAmount,
            BigDecimal blockedAmount,
            LocalDateTime updateDateTime
    ) {}

    public record ImportTransactionsCommand(
            String accountId,
            List<TransactionData> transactions,
            LocalDateTime importedAt
    ) {}

    public record TransactionData(
            String transactionId,
            String type,
            String creditDebitType,
            String transactionName,
            BigDecimal amount,
            LocalDateTime transactionDateTime
    ) {}
}

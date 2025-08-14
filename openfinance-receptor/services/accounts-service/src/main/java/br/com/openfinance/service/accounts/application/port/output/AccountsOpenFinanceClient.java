package br.com.openfinance.service.accounts.application.port.output;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de saída - Cliente OpenFinance para Accounts
 */
public interface AccountsOpenFinanceClient {

    AccountListResponse getAccounts(String organizationId, String accessToken);
    AccountDetailResponse getAccount(String organizationId, String accountId, String accessToken);
    BalanceResponse getBalance(String organizationId, String accountId, String accessToken);
    TransactionsResponse getTransactions(
            String organizationId,
            String accountId,
            String accessToken,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page
    );

    // Response DTOs
    record AccountListResponse(List<AccountData> accounts) {}
    record AccountDetailResponse(AccountData account) {}
    record BalanceResponse(BalanceData balance) {}
    record TransactionsResponse(List<TransactionData> transactions, Integer totalPages) {}

    record AccountData(
            String accountId,
            String compeCode,
            String branchCode,
            String number,
            String checkDigit,
            String type,
            String subtype
    ) {}

    record BalanceData(
            BigDecimal availableAmount,
            BigDecimal blockedAmount,
            BigDecimal automaticallyInvestedAmount,
            LocalDateTime updateDateTime
    ) {}

    record TransactionData(
            String transactionId,
            String type,
            String creditDebitType,
            String transactionName,
            BigDecimal amount,
            LocalDateTime transactionDateTime
    ) {}
}

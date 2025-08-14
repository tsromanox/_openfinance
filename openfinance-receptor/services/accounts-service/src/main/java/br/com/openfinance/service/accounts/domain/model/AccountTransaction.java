package br.com.openfinance.service.accounts.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade AccountTransaction - Representa uma transação da conta
 */
public class AccountTransaction {
    private final UUID id;
    private final String transactionId; // ID externo
    private final AccountId accountId;
    private final TransactionType type;
    private final CreditDebitType creditDebitType;
    private final String transactionName;
    private final BigDecimal amount;
    private final Currency currency;
    private final LocalDateTime transactionDateTime;
    private final String partieCnpjCpf;
    private final String partiePersonType;
    private final String partieCompeCode;
    private final String partieBranchCode;
    private final String partieNumber;
    private final CompletedAuthorisedPaymentType completedAuthorisedPaymentType;

    // Constructor, getters, builder similar pattern...
}

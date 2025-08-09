package br.com.openfinance.accounts.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTransaction {
    private UUID transactionId;
    private String externalTransactionId;
    private Account account;
    private TransactionType type;
    private CreditDebitType creditDebitType;
    private String transactionName;
    private BigDecimal amount;
    private String transactionCurrency;
    private LocalDateTime transactionDateTime;
    private String partieCnpjCpf;
    private String partiePersonType;
    private String partieCompeCode;
    private String partieBranchCode;
    private String partieNumber;
    private String partieCheckDigit;

    public boolean isCredit() {
        return creditDebitType == CreditDebitType.CREDITO;
    }

    public boolean isDebit() {
        return creditDebitType == CreditDebitType.DEBITO;
    }
}

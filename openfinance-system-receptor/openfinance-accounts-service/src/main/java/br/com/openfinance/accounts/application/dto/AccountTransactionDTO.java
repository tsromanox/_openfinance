package br.com.openfinance.accounts.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTransactionDTO {
    private UUID transactionId;
    private String type;
    private String creditDebitType;
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
}

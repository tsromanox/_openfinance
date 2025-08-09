package br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity;

import br.com.openfinance.accounts.domain.model.CreditDebitType;
import br.com.openfinance.accounts.domain.model.TransactionType;
import br.com.openfinance.core.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_transactions",
        indexes = {
                @Index(name = "idx_account_id", columnList = "account_id"),
                @Index(name = "idx_transaction_date", columnList = "transaction_datetime"),
                @Index(name = "idx_account_date", columnList = "account_id,transaction_datetime")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTransactionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "external_transaction_id", unique = true)
    private String externalTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "credit_debit_type", nullable = false)
    private CreditDebitType creditDebitType;

    @Column(name = "transaction_name")
    private String transactionName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "transaction_currency", length = 3)
    private String transactionCurrency = "BRL";

    @Column(name = "transaction_datetime", nullable = false)
    private LocalDateTime transactionDateTime;

    @Column(name = "partie_cnpj_cpf", length = 14)
    private String partieCnpjCpf;

    @Column(name = "partie_person_type", length = 20)
    private String partiePersonType;

    @Column(name = "partie_compe_code", length = 3)
    private String partieCompeCode;

    @Column(name = "partie_branch_code", length = 4)
    private String partieBranchCode;

    @Column(name = "partie_number", length = 20)
    private String partieNumber;

    @Column(name = "partie_check_digit", length = 2)
    private String partieCheckDigit;
}

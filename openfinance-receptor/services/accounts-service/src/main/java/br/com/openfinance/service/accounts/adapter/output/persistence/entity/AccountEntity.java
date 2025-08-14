package br.com.openfinance.service.accounts.adapter.output.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "account_id", unique = true, nullable = false)
    private String accountId;

    @Column(name = "consent_id", nullable = false)
    private UUID consentId;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "compe_code", length = 3)
    private String compeCode;

    @Column(name = "branch_code", length = 4)
    private String branchCode;

    @Column(name = "number", length = 20)
    private String number;

    @Column(name = "check_digit", length = 1)
    private String checkDigit;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountTypeEntity type;

    @Column(name = "subtype")
    @Enumerated(EnumType.STRING)
    private AccountSubTypeEntity subtype;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AccountStatusEntity status;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccountBalanceEntity> balances = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Version
    private Long version;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public UUID getConsentId() { return consentId; }
    public void setConsentId(UUID consentId) { this.consentId = consentId; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCompeCode() { return compeCode; }
    public void setCompeCode(String compeCode) { this.compeCode = compeCode; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getCheckDigit() { return checkDigit; }
    public void setCheckDigit(String checkDigit) { this.checkDigit = checkDigit; }

    public AccountTypeEntity getType() { return type; }
    public void setType(AccountTypeEntity type) { this.type = type; }

    public AccountSubTypeEntity getSubtype() { return subtype; }
    public void setSubtype(AccountSubTypeEntity subtype) { this.subtype = subtype; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public AccountStatusEntity getStatus() { return status; }
    public void setStatus(AccountStatusEntity status) { this.status = status; }

    public List<AccountBalanceEntity> getBalances() { return balances; }
    public void setBalances(List<AccountBalanceEntity> balances) { this.balances = balances; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }
}

enum AccountTypeEntity {
    CONTA_DEPOSITO_A_VISTA,
    CONTA_POUPANCA,
    CONTA_PAGAMENTO_PRE_PAGA
}

enum AccountSubTypeEntity {
    INDIVIDUAL,
    CONJUNTA_SIMPLES,
    CONJUNTA_SOLIDARIA
}

enum AccountStatusEntity {
    AVAILABLE,
    UNAVAILABLE,
    TEMPORARILY_UNAVAILABLE,
    PENDING_AUTHORISATION
}

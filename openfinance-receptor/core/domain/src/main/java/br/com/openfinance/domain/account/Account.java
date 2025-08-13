package br.com.openfinance.domain.account;

import br.com.openfinance.domain.exception.InvalidAccountException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Builder
@Getter
public class Account {
    private static final Pattern CNPJ_PATTERN = Pattern.compile("\\d{14}");
    
    private final UUID id;
    private final String accountId;
    private final String brandName;
    private final String companyCnpj;
    private final String type;
    private final String subtype;
    private final String number;
    private final String checkDigit;
    private final String agencyNumber;
    private final String agencyCheckDigit;
    private final Balance balance;
    private final UUID consentId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime lastSyncAt;
    
    // Business logic methods
    public boolean needsSync() {
        if (lastSyncAt == null) {
            return true;
        }
        // Considera que precisa sincronizar se passou mais de 1 hora desde a última sync
        return lastSyncAt.isBefore(LocalDateTime.now().minusHours(1));
    }
    
    public AccountNumber getAccountNumber() {
        return new AccountNumber(number, checkDigit);
    }
    
    public AgencyNumber getAgencyNumber() {
        return new AgencyNumber(agencyNumber, agencyCheckDigit);
    }
    
    public Account withUpdatedBalance(Balance newBalance) {
        return Account.builder()
                .id(this.id)
                .accountId(this.accountId)
                .brandName(this.brandName)
                .companyCnpj(this.companyCnpj)
                .type(this.type)
                .subtype(this.subtype)
                .number(this.number)
                .checkDigit(this.checkDigit)
                .agencyNumber(this.agencyNumber)
                .agencyCheckDigit(this.agencyCheckDigit)
                .balance(newBalance)
                .consentId(this.consentId)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .lastSyncAt(LocalDateTime.now())
                .build();
    }
    
    public boolean isValid() {
        try {
            validateAccount();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void validateAccount() {
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(brandName, "Brand name cannot be null");
        Objects.requireNonNull(companyCnpj, "Company CNPJ cannot be null");
        Objects.requireNonNull(type, "Account type cannot be null");
        Objects.requireNonNull(number, "Account number cannot be null");
        Objects.requireNonNull(checkDigit, "Check digit cannot be null");
        Objects.requireNonNull(agencyNumber, "Agency number cannot be null");
        Objects.requireNonNull(consentId, "Consent ID cannot be null");
        Objects.requireNonNull(createdAt, "Created at cannot be null");
        
        if (accountId.trim().isEmpty()) {
            throw new InvalidAccountException("Account ID cannot be empty");
        }
        
        if (brandName.trim().isEmpty()) {
            throw new InvalidAccountException("Brand name cannot be empty");
        }
        
        if (!CNPJ_PATTERN.matcher(companyCnpj.replaceAll("[^\\d]", "")).matches()) {
            throw new InvalidAccountException("Invalid CNPJ format: " + companyCnpj);
        }
        
        // Validate account and agency numbers using value objects
        new AccountNumber(number, checkDigit);
        new AgencyNumber(agencyNumber, agencyCheckDigit);
    }
}
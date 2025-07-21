package br.com.openfinance.accounts.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private String id;
    private String accountId;
    private String clientId;
    private String consentId;
    private String institutionId;
    private String brandName;
    private String companyCnpj;
    private String type;
    private String compeCode;
    private String branchCode;
    private String number;
    private String checkDigit;
    private AccountBalance balance;
    private AccountLimit limit;
    private LocalDateTime lastUpdated;
    private String status;
    private String partitionKey;

    public static Account create(String clientId, String consentId, String institutionId) {
        return Account.builder()
                .id(UUID.randomUUID().toString())
                .clientId(clientId)
                .consentId(consentId)
                .institutionId(institutionId)
                .status("PENDING")
                .partitionKey(generatePartitionKey(clientId, institutionId))
                .build();
    }

    private static String generatePartitionKey(String clientId, String institutionId) {
        // Estratégia de particionamento para distribuir dados uniformemente
        return String.format("%s:%s", clientId.substring(0, 3), institutionId);
    }
}

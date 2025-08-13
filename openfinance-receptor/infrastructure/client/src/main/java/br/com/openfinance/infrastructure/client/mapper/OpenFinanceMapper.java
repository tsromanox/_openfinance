package br.com.openfinance.infrastructure.client.mapper;

import br.com.openfinance.application.dto.AccountsResponse;
import br.com.openfinance.application.dto.BalanceResponse;
import br.com.openfinance.application.dto.ConsentRequest;
import br.com.openfinance.application.dto.ConsentResponse;
import br.com.openfinance.domain.account.Account;
import br.com.openfinance.domain.account.Balance;
import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.domain.consent.ConsentStatus;
import br.com.openfinance.domain.consent.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for converting between DTOs and domain objects with high-performance optimizations.
 */
@Mapper(componentModel = "spring", uses = {})
public interface OpenFinanceMapper {
    
    // Consent mappings
    @Mapping(target = "permissions", source = "permissions", qualifiedByName = "mapPermissionsToStrings")
    ConsentRequest toConsentRequest(Consent consent);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", source = "permissions", qualifiedByName = "mapStringPermissions")
    @Mapping(target = "status", source = "status", qualifiedByName = "mapConsentStatus")
    @Mapping(target = "createdAt", ignore = true) // Set by factory
    Consent toConsentDomain(ConsentResponse response);
    
    // Account mappings
    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "balance", source = ".", qualifiedByName = "mapAccountDataToBalance")
    @Mapping(target = "consentId", ignore = true) // Set by caller
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "lastSyncAt", expression = "java(java.time.LocalDateTime.now())")
    Account toAccountDomain(AccountsResponse.AccountData accountData);
    
    // Balance mappings
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    Balance toBalanceDomain(BalanceResponse.BalanceData balanceData);
    
    // Batch mappings for parallel processing
    List<Account> toAccountDomainList(List<AccountsResponse.AccountData> accountDataList);
    List<Balance> toBalanceDomainList(List<BalanceResponse.BalanceData> balanceDataList);
    
    // Custom mapping methods
    @Named("mapPermissionsToStrings")
    default Set<String> mapPermissionsToStrings(Set<Permission> permissions) {
        if (permissions == null) {
            return Set.of();
        }
        return permissions.stream()
                .map(Permission::name)
                .collect(Collectors.toSet());
    }
    
    @Named("mapStringPermissions")
    default Set<Permission> mapStringPermissions(Set<String> permissions) {
        if (permissions == null) {
            return Set.of();
        }
        return permissions.stream()
                .map(permStr -> {
                    try {
                        return Permission.valueOf(permStr);
                    } catch (IllegalArgumentException e) {
                        // Log warning and skip invalid permission
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }
    
    @Named("mapConsentStatus")
    default ConsentStatus mapConsentStatus(String status) {
        if (status == null) {
            return ConsentStatus.AWAITING_AUTHORISATION;
        }
        try {
            return ConsentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            // Default to awaiting if unknown status
            return ConsentStatus.AWAITING_AUTHORISATION;
        }
    }
    
    @Named("mapAccountDataToBalance")
    default Balance mapAccountDataToBalance(AccountsResponse.AccountData accountData) {
        return Balance.builder()
                .availableAmount(accountData.availableAmount())
                .availableAmountCurrency(accountData.availableAmountCurrency())
                .blockedAmount(accountData.blockedAmount())
                .blockedAmountCurrency(accountData.blockedAmountCurrency())
                .automaticallyInvestedAmount(accountData.automaticallyInvestedAmount())
                .automaticallyInvestedAmountCurrency(accountData.automaticallyInvestedAmountCurrency())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }
}
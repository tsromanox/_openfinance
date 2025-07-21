package br.com.openfinance.accounts.infrastructure.mapper;

import br.com.openfinance.accounts.domain.entity.Account;
import br.com.openfinance.accounts.domain.entity.AccountBalance;
import br.com.openfinance.accounts.domain.entity.AccountLimit;
import br.com.openfinance.accounts.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "consentId", ignore = true)
    @Mapping(target = "institutionId", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "limit", ignore = true)
    @Mapping(target = "lastUpdated", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "partitionKey", ignore = true)
    @Mapping(source = "type", target = "type", qualifiedByName = "enumToString")
    @Mapping(source = "compeCode", target = "compeCode")
    @Mapping(source = "branchCode", target = "branchCode")
    @Mapping(source = "number", target = "number")
    @Mapping(source = "checkDigit", target = "checkDigit")
    Account toDomainAccount(AccountIdentificationData data);

    @Mapping(source = "availableAmount", target = "availableAmount", qualifiedByName = "amountToBigDecimal")
    @Mapping(source = "availableAmount.currency", target = "availableAmountCurrency")
    @Mapping(source = "blockedAmount", target = "blockedAmount", qualifiedByName = "amountToBigDecimal")
    @Mapping(source = "blockedAmount.currency", target = "blockedAmountCurrency")
    @Mapping(source = "automaticallyInvestedAmount", target = "automaticallyInvestedAmount", qualifiedByName = "amountToBigDecimal")
    @Mapping(source = "automaticallyInvestedAmount.currency", target = "automaticallyInvestedAmountCurrency")
    @Mapping(source = "updateDateTime", target = "updateDateTime", qualifiedByName = "offsetToLocalDateTime")
    AccountBalance toDomainBalance(AccountBalancesData data);

    @Mapping(source = "overdraftContractedLimit", target = "overdraftContractedLimit", qualifiedByName = "limitToBigDecimal")
    @Mapping(source = "overdraftContractedLimit.currency", target = "overdraftContractedLimitCurrency")
    @Mapping(source = "overdraftUsedLimit", target = "overdraftUsedLimit", qualifiedByName = "limitToBigDecimal")
    @Mapping(source = "overdraftUsedLimit.currency", target = "overdraftUsedLimitCurrency")
    @Mapping(source = "unarrangedOverdraftAmount", target = "unarrangedOverdraftAmount", qualifiedByName = "limitToBigDecimal")
    @Mapping(source = "unarrangedOverdraftAmount.currency", target = "unarrangedOverdraftAmountCurrency")
    AccountLimit toDomainLimit(AccountOverdraftLimitsData data);

    @Named("enumToString")
    default String enumToString(EnumAccountType type) {
        return type != null ? type.name() : null;
    }

    @Named("amountToBigDecimal")
    default BigDecimal amountToBigDecimal(AccountBalancesDataAvailableAmount amount) {
        if (amount == null || amount.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(amount.getAmount());
    }

    @Named("amountToBigDecimal")
    default BigDecimal amountToBigDecimal(AccountBalancesDataBlockedAmount amount) {
        if (amount == null || amount.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(amount.getAmount());
    }

    @Named("amountToBigDecimal")
    default BigDecimal amountToBigDecimal(AccountBalancesDataAutomaticallyInvestedAmount amount) {
        if (amount == null || amount.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(amount.getAmount());
    }

    @Named("limitToBigDecimal")
    default BigDecimal limitToBigDecimal(AccountOverdraftLimitsDataOverdraftContractedLimit limit) {
        if (limit == null || limit.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(limit.getAmount());
    }

    @Named("limitToBigDecimal")
    default BigDecimal limitToBigDecimal(AccountOverdraftLimitsDataOverdraftUsedLimit limit) {
        if (limit == null || limit.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(limit.getAmount());
    }

    @Named("limitToBigDecimal")
    default BigDecimal limitToBigDecimal(AccountOverdraftLimitsDataUnarrangedOverdraftAmount limit) {
        if (limit == null || limit.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(limit.getAmount());
    }

    @Named("offsetToLocalDateTime")
    default LocalDateTime offsetToLocalDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDateTime() : null;
    }

    default OffsetDateTime localToOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
    }
}

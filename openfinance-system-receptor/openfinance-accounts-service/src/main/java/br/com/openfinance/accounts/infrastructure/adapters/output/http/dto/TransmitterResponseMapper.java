package br.com.openfinance.accounts.infrastructure.adapters.output.http.dto;

import br.com.openfinance.accounts.domain.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransmitterResponseMapper {

    @Mapping(source = "accountId", target = "externalAccountId")
    @Mapping(source = "balances.availableAmount", target = "availableAmount")
    @Mapping(source = "balances.blockedAmount", target = "blockedAmount")
    @Mapping(source = "balances.automaticallyInvestedAmount", target = "automaticallyInvestedAmount")
    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "participantId", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "balances", ignore = true)
    @Mapping(target = "lastUpdateDateTime", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "status", constant = "ACTIVE")
    Account toDomain(AccountResponse response);

    @Mapping(source = "transactionId", target = "externalTransactionId")
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "account", ignore = true)
    AccountTransaction toTransactionDomain(TransactionResponse response);

    @Mapping(target = "balanceId", ignore = true)
    @Mapping(target = "account", ignore = true)
    AccountBalance toBalanceDomain(BalanceResponse response);
}

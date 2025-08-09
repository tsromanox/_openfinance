package br.com.openfinance.accounts.infrastructure.adapters.output.persistence.mapper;

import br.com.openfinance.accounts.domain.model.*;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountPersistenceMapper {

    @Mapping(source = "transactions", target = "transactions", ignore = true)
    @Mapping(source = "balances", target = "balances", ignore = true)
    Account toDomain(AccountEntity entity);

    @Mapping(source = "transactions", target = "transactions", ignore = true)
    @Mapping(source = "balances", target = "balances", ignore = true)
    AccountEntity toEntity(Account domain);

    @Mapping(source = "account.accountId", target = "account.accountId")
    AccountTransaction toTransactionDomain(AccountTransactionEntity entity);

    @Mapping(source = "account.accountId", target = "account.accountId")
    AccountTransactionEntity toTransactionEntity(AccountTransaction domain);

    @Mapping(source = "account.accountId", target = "account.accountId")
    AccountBalance toBalanceDomain(AccountBalanceEntity entity);

    @Mapping(source = "account.accountId", target = "account.accountId")
    AccountBalanceEntity toBalanceEntity(AccountBalance domain);

    AccountIdentification toIdentificationDomain(AccountIdentificationEntity entity);
    AccountIdentificationEntity toIdentificationEntity(AccountIdentification domain);

    void updateEntityFromDomain(Account domain, @MappingTarget AccountEntity entity);
}

package br.com.openfinance.accounts.application.mapper;

import br.com.openfinance.accounts.application.dto.*;
import br.com.openfinance.accounts.domain.model.*;
import br.com.openfinance.core.application.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper extends BaseMapper<Account, AccountDTO> {

    @Mapping(target = "type", expression = "java(account.getType().getValue())")
    @Mapping(target = "subtype", expression = "java(account.getSubtype().getValue())")
    AccountDTO toDto(Account account);

    @Mapping(target = "type", expression = "java(AccountType.valueOf(dto.getType()))")
    @Mapping(target = "subtype", expression = "java(AccountSubType.valueOf(dto.getSubtype()))")
    Account toEntity(AccountDTO dto);

    AccountTransactionDTO toTransactionDto(AccountTransaction transaction);
    AccountTransaction toTransactionEntity(AccountTransactionDTO dto);

    AccountBalanceDTO toBalanceDto(AccountBalance balance);
    AccountBalance toBalanceEntity(AccountBalanceDTO dto);

    AccountIdentificationDTO toIdentificationDto(AccountIdentification identification);
    AccountIdentification toIdentificationEntity(AccountIdentificationDTO dto);

    @Mapping(target = "accountId", ignore = true)
    @Mapping(target = "externalAccountId", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "balances", ignore = true)
    Account fromCreateRequest(CreateAccountRequest request);

    void updateAccountFromDto(AccountDTO dto, @MappingTarget Account account);
}

package br.com.openfinance.service.accounts.adapter.input.rest.mapper;


import br.com.openfinance.domain.account.Account;
import br.com.openfinance.service.accounts.adapter.input.rest.dto.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AccountMapper {

    public AccountDto toDto(Account account) {
        return new AccountDto(
                account.getAccountId(),
                "Open Finance Bank", // From config
                "12345678901234", // From config
                account.getType().name(),
                account.getIdentification().getCompeCode(),
                account.getIdentification().getBranchCode(),
                account.getIdentification().getNumber(),
                account.getIdentification().getCheckDigit()
        );
    }

    public AccountDetailDto toDetailDto(Account account) {
        return new AccountDetailDto(
                account.getIdentification().getCompeCode(),
                account.getIdentification().getBranchCode(),
                account.getIdentification().getNumber(),
                account.getIdentification().getCheckDigit(),
                account.getType().name(),
                account.getSubtype().name(),
                account.getCurrency().name()
        );
    }

    public BalanceDto toBalanceDto(AccountBalance balance) {
        return new BalanceDto(
                toMoneyDto(balance.getAvailableAmount(), balance.getCurrency()),
                toMoneyDto(balance.getBlockedAmount(), balance.getCurrency()),
                toMoneyDto(balance.getAutomaticallyInvestedAmount(), balance.getCurrency()),
                balance.getUpdateDateTime()
        );
    }

    public TransactionDto toTransactionDto(AccountTransaction transaction) {
        return new TransactionDto(
                transaction.getTransactionId(),
                transaction.getCompletedAuthorisedPaymentType().name(),
                transaction.getCreditDebitType().name(),
                transaction.getTransactionName(),
                transaction.getType().name(),
                toMoneyDto(transaction.getAmount(), transaction.getCurrency()),
                transaction.getTransactionDateTime(),
                transaction.getPartieCnpjCpf()
        );
    }

    private MoneyDto toMoneyDto(BigDecimal amount, Currency currency) {
        return new MoneyDto(
                amount != null ? amount.toString() : "0.00",
                currency.name()
        );
    }
}

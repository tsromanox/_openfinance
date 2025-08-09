package br.com.openfinance.accounts.domain.services;

import br.com.openfinance.accounts.domain.model.*;
import br.com.openfinance.accounts.domain.ports.output.AccountEventPublisher;
import br.com.openfinance.core.domain.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountDomainService {

    private final AccountEventPublisher eventPublisher;

    public void validateAccount(Account account) {
        if (account.getCustomerId() == null || account.getCustomerId().isBlank()) {
            throw new BusinessException("Customer ID is required", "INVALID_ACCOUNT");
        }

        if (account.getNumber() == null || account.getNumber().isBlank()) {
            throw new BusinessException("Account number is required", "INVALID_ACCOUNT");
        }

        if (account.getType() == null) {
            throw new BusinessException("Account type is required", "INVALID_ACCOUNT");
        }

        validateBalances(account);
    }

    private void validateBalances(Account account) {
        BigDecimal available = account.getAvailableAmount();
        BigDecimal blocked = account.getBlockedAmount();
        BigDecimal invested = account.getAutomaticallyInvestedAmount();

        if (available == null || available.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Available amount cannot be negative", "INVALID_BALANCE");
        }

        if (blocked == null || blocked.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Blocked amount cannot be negative", "INVALID_BALANCE");
        }

        if (invested == null || invested.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Invested amount cannot be negative", "INVALID_BALANCE");
        }
    }

    public void updateAccountFromExternal(Account account, Account externalData) {
        account.setAvailableAmount(externalData.getAvailableAmount());
        account.setBlockedAmount(externalData.getBlockedAmount());
        account.setAutomaticallyInvestedAmount(externalData.getAutomaticallyInvestedAmount());
        account.setOverdraftContractedLimit(externalData.getOverdraftContractedLimit());
        account.setOverdraftUsedLimit(externalData.getOverdraftUsedLimit());
        account.setUnarrangedOverdraftAmount(externalData.getUnarrangedOverdraftAmount());
        account.setLastUpdateDateTime(LocalDateTime.now());
        account.syncCompleted();

        log.info("Account {} updated from external source", account.getAccountId());
        eventPublisher.publishAccountUpdated(account);
    }

    public void processTransactions(Account account, List<AccountTransaction> transactions) {
        transactions.forEach(transaction -> {
            transaction.setAccount(account);
            account.addTransaction(transaction);
        });

        log.info("Processed {} transactions for account {}",
                transactions.size(), account.getAccountId());
    }

    public AccountBalance createBalanceSnapshot(Account account) {
        return AccountBalance.builder()
                .account(account)
                .availableAmount(account.getAvailableAmount())
                .blockedAmount(account.getBlockedAmount())
                .automaticallyInvestedAmount(account.getAutomaticallyInvestedAmount())
                .referenceDateTime(LocalDateTime.now())
                .build();
    }

    public boolean shouldSyncAccount(Account account) {
        if (account.getSyncedAt() == null) {
            return true;
        }

        LocalDateTime lastSync = account.getSyncedAt();
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);

        return lastSync.isBefore(threshold);
    }

    public void validateTransaction(AccountTransaction transaction) {
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Transaction amount must be positive", "INVALID_TRANSACTION");
        }

        if (transaction.getType() == null) {
            throw new BusinessException("Transaction type is required", "INVALID_TRANSACTION");
        }

        if (transaction.getCreditDebitType() == null) {
            throw new BusinessException("Credit/Debit type is required", "INVALID_TRANSACTION");
        }
    }
}

package br.com.openfinance.accounts.domain.ports.output;

import br.com.openfinance.accounts.domain.model.AccountTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface TransactionRepository {
    AccountTransaction save(AccountTransaction transaction);
    Optional<AccountTransaction> findById(UUID transactionId);
    Page<AccountTransaction> findByAccountId(UUID accountId, Pageable pageable);
    Page<AccountTransaction> findByAccountIdAndPeriod(
            UUID accountId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );
    List<AccountTransaction> saveAll(List<AccountTransaction> transactions);
    void deleteByAccountId(UUID accountId);
}
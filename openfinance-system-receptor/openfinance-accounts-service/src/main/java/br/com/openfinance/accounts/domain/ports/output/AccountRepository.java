package br.com.openfinance.accounts.domain.ports.output;

import br.com.openfinance.accounts.domain.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(UUID accountId);
    Optional<Account> findByExternalId(String participantId, String externalAccountId);
    Page<Account> findByCustomerId(String customerId, Pageable pageable);
    Page<Account> findByParticipantId(String participantId, Pageable pageable);
    List<Account> findAccountsForBatchUpdate(int batchSize);
    long count();
    void deleteById(UUID accountId);

    // Add these methods for Virtual Thread processing
    List<Account> findAll(Pageable pageable);
    Page<Account> findAllActive(Pageable pageable);
    List<Account> findAccountsNeedingSync(int limit);
}

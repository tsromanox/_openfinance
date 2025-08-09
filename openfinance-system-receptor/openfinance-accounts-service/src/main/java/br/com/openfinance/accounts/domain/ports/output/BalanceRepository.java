package br.com.openfinance.accounts.domain.ports.output;

import br.com.openfinance.accounts.domain.model.AccountBalance;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface BalanceRepository {
    AccountBalance save(AccountBalance balance);
    Optional<AccountBalance> findLatestByAccountId(UUID accountId);
    List<AccountBalance> findByAccountId(UUID accountId);
    void deleteByAccountId(UUID accountId);
}

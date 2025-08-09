package br.com.openfinance.accounts.domain.ports.input;

import br.com.openfinance.accounts.domain.model.AccountBalance;
import java.util.Optional;
import java.util.UUID;

public interface GetAccountBalanceUseCase {
    Optional<AccountBalance> getCurrentBalance(UUID accountId);
}

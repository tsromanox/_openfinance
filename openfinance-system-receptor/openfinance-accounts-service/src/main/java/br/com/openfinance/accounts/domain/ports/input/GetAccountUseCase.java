package br.com.openfinance.accounts.domain.ports.input;

import br.com.openfinance.accounts.domain.model.Account;
import java.util.Optional;
import java.util.UUID;

public interface GetAccountUseCase {
    Optional<Account> getAccount(UUID accountId);
    Optional<Account> getAccountByExternalId(String participantId, String externalAccountId);
}

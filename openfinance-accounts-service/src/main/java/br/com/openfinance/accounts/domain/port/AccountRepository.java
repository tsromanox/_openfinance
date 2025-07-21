package br.com.openfinance.accounts.domain.port;

import br.com.openfinance.accounts.domain.entity.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountRepository {
    Mono<Account> save(Account account);
    Mono<Account> findById(String id);
    Flux<Account> findByClientId(String clientId);
    Flux<Account> findByConsentId(String consentId);
    Flux<Account> findAccountsForUpdate(int limit);
    Mono<Long> countByClientId(String clientId);
}

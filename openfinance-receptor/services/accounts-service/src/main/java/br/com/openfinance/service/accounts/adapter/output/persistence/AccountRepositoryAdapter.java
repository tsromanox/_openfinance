package br.com.openfinance.service.accounts.adapter.output.persistence;

import br.com.openfinance.accounts.adapter.output.persistence.entity.AccountEntity;
import br.com.openfinance.accounts.adapter.output.persistence.repository.AccountJpaRepository;
import br.com.openfinance.accounts.application.port.output.AccountRepository;
import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.valueobject.AccountId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;
    private final AccountPersistenceMapper mapper;

    public AccountRepositoryAdapter(
            AccountJpaRepository jpaRepository,
            AccountPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Account save(Account account) {
        var entity = mapper.toEntity(account);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Account> saveAll(List<Account> accounts) {
        var entities = accounts.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());

        var saved = jpaRepository.saveAll(entities);

        return saved.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Account> findById(AccountId accountId) {
        return jpaRepository.findById(accountId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountId(String accountId) {
        return jpaRepository.findByAccountId(accountId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Account> findByConsentId(UUID consentId) {
        return jpaRepository.findByConsentId(consentId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Account> findByCustomerId(String customerId) {
        return jpaRepository.findByCustomerId(customerId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Account> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Account> findAccountsNeedingSync(int limit) {
        var cutoffTime = LocalDateTime.now().minusMinutes(15);
        return jpaRepository.findAccountsNeedingSync(cutoffTime, limit).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByAccountId(String accountId) {
        return jpaRepository.existsByAccountId(accountId);
    }

    @Override
    public void updateLastSyncBatch(List<String> accountIds) {
        jpaRepository.updateLastSyncBatch(accountIds, LocalDateTime.now());
    }

    @Override
    public void delete(AccountId accountId) {
        jpaRepository.deleteById(accountId.getValue());
    }
}

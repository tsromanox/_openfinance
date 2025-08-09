package br.com.openfinance.accounts.infrastructure.adapters.output.persistence;

import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.model.AccountStatus;
import br.com.openfinance.accounts.domain.ports.output.AccountRepository;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity.AccountEntity;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.mapper.AccountPersistenceMapper;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.repository.AccountJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;
    private final AccountPersistenceMapper mapper;

    @Override
    public Account save(Account account) {
        AccountEntity entity = mapper.toEntity(account);
        entity = jpaRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return jpaRepository.findById(accountId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByExternalId(String participantId, String externalAccountId) {
        return jpaRepository.findByExternalAccountIdAndParticipantId(externalAccountId, participantId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Account> findByCustomerId(String customerId, Pageable pageable) {
        return jpaRepository.findByCustomerId(customerId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Account> findByParticipantId(String participantId, Pageable pageable) {
        return jpaRepository.findByParticipantId(participantId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public List<Account> findAccountsForBatchUpdate(int batchSize) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        PageRequest pageRequest = PageRequest.of(0, batchSize);

        List<AccountEntity> entities = jpaRepository.findAccountsForBatchUpdate(threshold, pageRequest);
        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public void deleteById(UUID accountId) {
        jpaRepository.deleteById(accountId);
    }

    @Override
    public List<Account> findAll(Pageable pageable) {
        Page<AccountEntity> entities = jpaRepository.findAll(pageable);
        return entities.getContent().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Account> findAllActive(Pageable pageable) {
        Page<AccountEntity> entities = jpaRepository.findByStatus(AccountStatus.ACTIVE, pageable);
        return entities.map(mapper::toDomain);
    }

    @Override
    public List<Account> findAccountsNeedingSync(int limit) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        PageRequest pageRequest = PageRequest.of(0, limit);

        List<AccountEntity> entities = jpaRepository.findAccountsForSync(threshold, pageRequest);
        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }
}
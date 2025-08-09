package br.com.openfinance.accounts.infrastructure.adapters.output.persistence;

import br.com.openfinance.accounts.domain.model.AccountTransaction;
import br.com.openfinance.accounts.domain.ports.output.TransactionRepository;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity.AccountTransactionEntity;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.mapper.AccountPersistenceMapper;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.repository.TransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;
    private final AccountPersistenceMapper mapper;

    @Override
    public AccountTransaction save(AccountTransaction transaction) {
        AccountTransactionEntity entity = mapper.toTransactionEntity(transaction);
        entity = jpaRepository.save(entity);
        return mapper.toTransactionDomain(entity);
    }

    @Override
    public Optional<AccountTransaction> findById(UUID transactionId) {
        return jpaRepository.findById(transactionId)
                .map(mapper::toTransactionDomain);
    }

    @Override
    public Page<AccountTransaction> findByAccountId(UUID accountId, Pageable pageable) {
        return jpaRepository.findByAccountAccountId(accountId, pageable)
                .map(mapper::toTransactionDomain);
    }

    @Override
    public Page<AccountTransaction> findByAccountIdAndPeriod(
            UUID accountId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        return jpaRepository.findByAccountIdAndPeriod(accountId, fromDate, toDate, pageable)
                .map(mapper::toTransactionDomain);
    }

    @Override
    public List<AccountTransaction> saveAll(List<AccountTransaction> transactions) {
        List<AccountTransactionEntity> entities = transactions.stream()
                .map(mapper::toTransactionEntity)
                .toList();

        entities = jpaRepository.saveAll(entities);

        return entities.stream()
                .map(mapper::toTransactionDomain)
                .toList();
    }

    @Override
    public void deleteByAccountId(UUID accountId) {
        jpaRepository.deleteByAccountAccountId(accountId);
    }
}

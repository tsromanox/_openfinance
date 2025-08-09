package br.com.openfinance.accounts.infrastructure.adapters.output.persistence;

import br.com.openfinance.accounts.domain.model.AccountBalance;
import br.com.openfinance.accounts.domain.ports.output.BalanceRepository;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity.AccountBalanceEntity;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.mapper.AccountPersistenceMapper;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.repository.BalanceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BalanceRepositoryAdapter implements BalanceRepository {

    private final BalanceJpaRepository jpaRepository;
    private final AccountPersistenceMapper mapper;

    @Override
    public AccountBalance save(AccountBalance balance) {
        AccountBalanceEntity entity = mapper.toBalanceEntity(balance);
        entity = jpaRepository.save(entity);
        return mapper.toBalanceDomain(entity);
    }

    @Override
    public Optional<AccountBalance> findLatestByAccountId(UUID accountId) {
        return jpaRepository.findLatestByAccountId(accountId)
                .map(mapper::toBalanceDomain);
    }

    @Override
    public List<AccountBalance> findByAccountId(UUID accountId) {
        return jpaRepository.findByAccountAccountId(accountId).stream()
                .map(mapper::toBalanceDomain)
                .toList();
    }

    @Override
    public void deleteByAccountId(UUID accountId) {
        jpaRepository.deleteByAccountAccountId(accountId);
    }
}

package br.com.openfinance.accounts.infrastructure.adapters.output.persistence.repository;

import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity.AccountBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BalanceJpaRepository extends JpaRepository<AccountBalanceEntity, UUID> {

    @Query("SELECT b FROM AccountBalanceEntity b " +
            "WHERE b.account.accountId = :accountId " +
            "ORDER BY b.referenceDateTime DESC " +
            "LIMIT 1")
    Optional<AccountBalanceEntity> findLatestByAccountId(@Param("accountId") UUID accountId);

    List<AccountBalanceEntity> findByAccountAccountId(UUID accountId);

    void deleteByAccountAccountId(UUID accountId);
}

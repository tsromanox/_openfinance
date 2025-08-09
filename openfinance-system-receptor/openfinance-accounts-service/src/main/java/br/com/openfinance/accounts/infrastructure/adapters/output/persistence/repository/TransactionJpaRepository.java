package br.com.openfinance.accounts.infrastructure.adapters.output.persistence.repository;

import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity.AccountTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TransactionJpaRepository extends JpaRepository<AccountTransactionEntity, UUID> {

    Page<AccountTransactionEntity> findByAccountAccountId(UUID accountId, Pageable pageable);

    @Query("SELECT t FROM AccountTransactionEntity t " +
            "WHERE t.account.accountId = :accountId " +
            "AND t.transactionDateTime BETWEEN :fromDate AND :toDate " +
            "ORDER BY t.transactionDateTime DESC")
    Page<AccountTransactionEntity> findByAccountIdAndPeriod(
            @Param("accountId") UUID accountId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    void deleteByAccountAccountId(UUID accountId);

    boolean existsByExternalTransactionId(String externalTransactionId);
}

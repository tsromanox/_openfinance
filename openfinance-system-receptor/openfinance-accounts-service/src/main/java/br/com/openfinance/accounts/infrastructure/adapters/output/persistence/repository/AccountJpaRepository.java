package br.com.openfinance.accounts.infrastructure.adapters.output.persistence.repository;

import br.com.openfinance.accounts.domain.model.AccountStatus;
import br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity.AccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByExternalAccountIdAndParticipantId(
            String externalAccountId, String participantId);

    Page<AccountEntity> findByCustomerId(String customerId, Pageable pageable);

    Page<AccountEntity> findByParticipantId(String participantId, Pageable pageable);

    Page<AccountEntity> findByStatus(AccountStatus status, Pageable pageable);

    @Query("SELECT a FROM AccountEntity a WHERE a.syncedAt < :threshold OR a.syncedAt IS NULL")
    List<AccountEntity> findAccountsForSync(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    @Query("SELECT a FROM AccountEntity a WHERE a.status = 'ACTIVE' " +
            "AND (a.syncedAt IS NULL OR a.syncedAt < :threshold) " +
            "ORDER BY a.syncedAt ASC NULLS FIRST")
    List<AccountEntity> findAccountsForBatchUpdate(
            @Param("threshold") LocalDateTime threshold, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AccountEntity a WHERE a.status = 'ACTIVE'")
    long countActive();
}

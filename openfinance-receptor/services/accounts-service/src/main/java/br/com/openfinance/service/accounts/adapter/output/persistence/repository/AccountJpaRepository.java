package br.com.openfinance.service.accounts.adapter.output.persistence.repository;

import br.com.openfinance.accounts.adapter.output.persistence.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByAccountId(String accountId);

    List<AccountEntity> findByConsentId(UUID consentId);

    List<AccountEntity> findByCustomerId(String customerId);

    List<AccountEntity> findByOrganizationId(String organizationId);

    @Query("""
        SELECT a FROM AccountEntity a 
        WHERE a.status = 'AVAILABLE' 
        AND (a.lastSyncAt IS NULL OR a.lastSyncAt < :cutoffTime)
        ORDER BY a.lastSyncAt ASC NULLS FIRST
        LIMIT :limit
        """)
    List<AccountEntity> findAccountsNeedingSync(
            @Param("cutoffTime") LocalDateTime cutoffTime,
            @Param("limit") int limit
    );

    boolean existsByAccountId(String accountId);

    @Modifying
    @Query("""
        UPDATE AccountEntity a 
        SET a.lastSyncAt = :syncTime 
        WHERE a.accountId IN :accountIds
        """)
    void updateLastSyncBatch(
            @Param("accountIds") List<String> accountIds,
            @Param("syncTime") LocalDateTime syncTime
    );

    @Query("""
        SELECT COUNT(a) FROM AccountEntity a 
        WHERE a.consentId = :consentId 
        AND a.status = 'AVAILABLE'
        """)
    long countActiveAccountsByConsent(@Param("consentId") UUID consentId);
}

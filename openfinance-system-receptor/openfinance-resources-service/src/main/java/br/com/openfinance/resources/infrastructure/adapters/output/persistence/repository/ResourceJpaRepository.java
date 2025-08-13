package br.com.openfinance.resources.infrastructure.adapters.output.persistence.repository;

import br.com.openfinance.resources.infrastructure.adapters.output.persistence.entity.ResourceEntity;
import br.com.openfinance.resources.domain.model.ResourceType;
import br.com.openfinance.resources.domain.model.ResourceStatus;
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
public interface ResourceJpaRepository extends JpaRepository<ResourceEntity, UUID> {
    
    Optional<ResourceEntity> findByParticipantIdAndExternalResourceId(String participantId, String externalResourceId);
    
    Page<ResourceEntity> findByCustomerId(String customerId, Pageable pageable);
    
    Page<ResourceEntity> findByParticipantId(String participantId, Pageable pageable);
    
    Page<ResourceEntity> findByType(ResourceType type, Pageable pageable);
    
    Page<ResourceEntity> findByCustomerIdAndType(String customerId, ResourceType type, Pageable pageable);
    
    Page<ResourceEntity> findByStatus(ResourceStatus status, Pageable pageable);
    
    @Query("""
        SELECT r FROM ResourceEntity r 
        WHERE r.status = 'ACTIVE' 
        AND (r.lastSyncAt IS NULL OR r.lastSyncAt < :cutoffTime)
        ORDER BY r.lastSyncAt ASC NULLS FIRST
        """)
    List<ResourceEntity> findResourcesForBatchUpdate(@Param("cutoffTime") LocalDateTime cutoffTime, Pageable pageable);
    
    @Query("""
        SELECT r FROM ResourceEntity r 
        WHERE r.status = 'ACTIVE' 
        AND (r.lastSyncAt IS NULL OR r.lastSyncAt < :cutoffTime)
        ORDER BY r.lastSyncAt ASC NULLS FIRST
        """)
    List<ResourceEntity> findResourcesNeedingSync(@Param("cutoffTime") LocalDateTime cutoffTime, Pageable pageable);
    
    @Query("""
        SELECT COUNT(r) FROM ResourceEntity r 
        WHERE r.customerId = :customerId AND r.status = 'ACTIVE'
        """)
    Long countActiveResourcesByCustomer(@Param("customerId") String customerId);
    
    @Query("""
        SELECT COUNT(r) FROM ResourceEntity r 
        WHERE r.participantId = :participantId AND r.status = 'ACTIVE'
        """)
    Long countActiveResourcesByParticipant(@Param("participantId") String participantId);
    
    @Query("""
        SELECT DISTINCT r.customerId FROM ResourceEntity r 
        WHERE r.participantId = :participantId AND r.status = 'ACTIVE'
        """)
    List<String> findActiveCustomersByParticipant(@Param("participantId") String participantId);
    
    @Query("""
        SELECT r FROM ResourceEntity r 
        WHERE r.customerId = :customerId 
        AND r.type IN :types 
        AND r.status = 'ACTIVE'
        """)
    List<ResourceEntity> findActiveResourcesByCustomerAndTypes(
        @Param("customerId") String customerId, 
        @Param("types") List<ResourceType> types
    );
}
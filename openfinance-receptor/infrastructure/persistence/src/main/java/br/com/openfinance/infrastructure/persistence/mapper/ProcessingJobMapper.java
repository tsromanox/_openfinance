package br.com.openfinance.infrastructure.persistence.mapper;

import br.com.openfinance.domain.processing.ProcessingJob;
import br.com.openfinance.infrastructure.persistence.entity.ProcessingJobEntity;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * High-performance mapper for ProcessingJob domain objects with optimized conversions.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProcessingJobMapper {
    
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    ProcessingJobEntity toEntity(ProcessingJob domain);
    
    ProcessingJob toDomain(ProcessingJobEntity entity);
    
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    ProcessingJobEntity updateEntityFromDomain(ProcessingJob domain, @MappingTarget ProcessingJobEntity entity);
    
    // Batch conversion methods for performance
    List<ProcessingJob> toDomainList(List<ProcessingJobEntity> entities);
    
    List<ProcessingJobEntity> toEntityList(List<ProcessingJob> domains);
    
    // Custom mapping for performance metrics
    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "actualDurationMs", source = "entity.actualDurationMs")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    JobPerformanceMetrics toPerformanceMetrics(ProcessingJobEntity entity);
    
    /**
     * Performance metrics record for monitoring.
     */
    record JobPerformanceMetrics(
            Long id,
            String status,
            Long actualDurationMs,
            LocalDateTime createdAt
    ) {}
}
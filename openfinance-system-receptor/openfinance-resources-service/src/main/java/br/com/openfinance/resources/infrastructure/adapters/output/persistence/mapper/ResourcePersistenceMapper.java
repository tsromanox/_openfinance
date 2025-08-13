package br.com.openfinance.resources.infrastructure.adapters.output.persistence.mapper;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceMetadata;
import br.com.openfinance.resources.infrastructure.adapters.output.persistence.entity.ResourceEntity;
import br.com.openfinance.resources.infrastructure.adapters.output.persistence.entity.ResourceMetadataEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ResourcePersistenceMapper {
    
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    Resource toDomain(ResourceEntity entity);
    
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ResourceEntity toEntity(Resource domain);
    
    ResourceMetadata toDomain(ResourceMetadataEntity entity);
    ResourceMetadataEntity toEntity(ResourceMetadata domain);
    
    void updateEntityFromDomain(Resource domain, @MappingTarget ResourceEntity entity);
    void updateDomainFromEntity(ResourceEntity entity, @MappingTarget Resource domain);
}
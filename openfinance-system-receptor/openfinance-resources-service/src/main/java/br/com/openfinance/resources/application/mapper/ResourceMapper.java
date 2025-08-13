package br.com.openfinance.resources.application.mapper;

import br.com.openfinance.resources.application.dto.CreateResourceRequest;
import br.com.openfinance.resources.application.dto.ResourceDTO;
import br.com.openfinance.resources.application.dto.ResourceMetadataDTO;
import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ResourceMapper {
    
    ResourceDTO toDTO(Resource resource);
    Resource toEntity(ResourceDTO dto);
    
    @Mapping(target = "resourceId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastSyncAt", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    Resource toEntity(CreateResourceRequest request);
    
    ResourceMetadataDTO toDTO(ResourceMetadata metadata);
    ResourceMetadata toEntity(ResourceMetadataDTO dto);
    
    void updateEntityFromDTO(ResourceDTO dto, @MappingTarget Resource entity);
}
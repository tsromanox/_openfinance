package br.com.openfinance.consents.infrastructure.adapter.persistence.mapper;

import br.com.openfinance.consents.domain.entity.*;
import br.com.openfinance.consents.infrastructure.adapter.persistence.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ConsentMapper {

    @Mapping(target = "id", source = "consentId")
    @Mapping(target = "partitionKey", source = "clientId")
    ConsentEntity toEntity(Consent consent);

    @Mapping(target = "consentId", source = "id")
    @Mapping(target = "clientId", source = "partitionKey")
    Consent toDomain(ConsentEntity entity);

    List<ConsentPermissionEntity> toPermissionEntities(List<ConsentPermission> permissions);
    List<ConsentPermission> toPermissionDomains(List<ConsentPermissionEntity> entities);

    ConsentRejectionReasonEntity toRejectionEntity(ConsentRejectionReason reason);
    ConsentRejectionReason toRejectionDomain(ConsentRejectionReasonEntity entity);
}

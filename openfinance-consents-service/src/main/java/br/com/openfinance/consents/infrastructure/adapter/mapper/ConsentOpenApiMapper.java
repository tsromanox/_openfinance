package br.com.openfinance.consents.infrastructure.adapter.mapper;

import br.com.openfinance.consents.domain.entity.*;

import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface ConsentOpenApiMapper {

    // Request mappings
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "organisationId", ignore = true)
    @Mapping(target = "loggedUserDocument", source = "data.loggedUser.document.identification")
    @Mapping(target = "loggedUserDocumentRel", source = "data.loggedUser.document.rel")
    @Mapping(target = "businessEntityDocument", source = "data.businessEntity.document.identification")
    @Mapping(target = "businessEntityDocumentRel", source = "data.businessEntity.document.rel")
    @Mapping(target = "permissions", source = "data.permissions")
    @Mapping(target = "expirationDateTime", source = "data.expirationDateTime")
    @Mapping(target = "transactionFromDateTime", ignore = true)
    @Mapping(target = "transactionToDateTime", ignore = true)
    @Mapping(target = "token", ignore = true)
    CreateConsentCommand toCreateCommand(CreateConsent request);

    // Response mappings
    @Mapping(target = "data.consentId", source = "consentId")
    @Mapping(target = "data.creationDateTime", source = "creationDateTime")
    @Mapping(target = "data.status", source = "status")
    @Mapping(target = "data.statusUpdateDateTime", source = "statusUpdateDateTime")
    @Mapping(target = "data.permissions", source = "permissions")
    @Mapping(target = "data.expirationDateTime", source = "expirationDateTime")
    @Mapping(target = "links.self", expression = "java(buildSelfLink(consent.getConsentId()))")
    @Mapping(target = "meta.requestDateTime", expression = "java(getCurrentDateTime())")
    ResponseConsent toResponseConsent(Consent consent);

    @Mapping(target = "data.consentId", source = "consentId")
    @Mapping(target = "data.creationDateTime", source = "creationDateTime")
    @Mapping(target = "data.status", source = "status")
    @Mapping(target = "data.statusUpdateDateTime", source = "statusUpdateDateTime")
    @Mapping(target = "data.permissions", source = "permissions")
    @Mapping(target = "data.expirationDateTime", source = "expirationDateTime")
    @Mapping(target = "data.rejection", source = "rejectionReason")
    @Mapping(target = "links.self", expression = "java(buildSelfLink(consent.getConsentId()))")
    @Mapping(target = "meta.requestDateTime", expression = "java(getCurrentDateTime())")
    ResponseConsentRead toResponseConsentRead(Consent consent);

    // Enum mappings
    default ResponseConsentData.StatusEnum mapStatus(ConsentStatus status) {
        return switch (status) {
            case AUTHORISED -> ResponseConsentData.StatusEnum.AUTHORISED;
            case AWAITING_AUTHORISATION -> ResponseConsentData.StatusEnum.AWAITING_AUTHORISATION;
            case REJECTED -> ResponseConsentData.StatusEnum.REJECTED;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }

    default ConsentStatus mapStatus(ResponseConsentData.StatusEnum status) {
        return switch (status) {
            case AUTHORISED -> ConsentStatus.AUTHORISED;
            case AWAITING_AUTHORISATION -> ConsentStatus.AWAITING_AUTHORISATION;
            case REJECTED -> ConsentStatus.REJECTED;
        };
    }

    // Permission mappings
    default List<String> mapPermissions(List<ConsentPermission> permissions) {
        if (permissions == null) return List.of();
        return permissions.stream()
                .map(p -> p.getType().name())
                .collect(Collectors.toList());
    }

    default List<CreateConsentData.PermissionsEnum> mapPermissionEnums(List<ConsentPermission> permissions) {
        if (permissions == null) return List.of();
        return permissions.stream()
                .map(p -> CreateConsentData.PermissionsEnum.fromValue(p.getType().name()))
                .collect(Collectors.toList());
    }

    // Rejection mapping
    @Mapping(target = "rejectedBy", source = "code")
    @Mapping(target = "reason.code", source = "code")
    @Mapping(target = "reason.additionalInformation", source = "additionalInformation")
    ResponseConsentReadDataRejection toRejection(ConsentRejectionReason rejectionReason);

    default EnumRejectedBy mapRejectedBy(RejectionCode code) {
        return switch (code) {
            case CUSTOMER_MANUALLY_REJECTED, CUSTOMER_MANUALLY_REVOKED -> EnumRejectedBy.USER;
            case CONSENT_EXPIRED, CONSENT_MAX_DATE_REACHED, CONSENT_REVOKED_BY_ASPSP -> EnumRejectedBy.ASPSP;
            case CONSENT_REVOKED_BY_CLIENT -> EnumRejectedBy.TPP;
            default -> EnumRejectedBy.ASPSP;
        };
    }

    // Helper methods
    default String buildSelfLink(String consentId) {
        return "/consents/v3/consents/" + consentId;
    }

    default OffsetDateTime getCurrentDateTime() {
        return LocalDateTime.now().atOffset(ZoneOffset.UTC);
    }

    default LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDateTime() : null;
    }

    default OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atOffset(ZoneOffset.UTC) : null;
    }
}

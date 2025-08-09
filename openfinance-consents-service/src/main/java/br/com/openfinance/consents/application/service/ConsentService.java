package br.com.openfinance.consents.application.service;


import br.com.openfinance.consents.domain.entity.ConsentRejectionReason;
import br.com.openfinance.consents.domain.entity.ConsentStatus;
import br.com.openfinance.consents.domain.entity.RejectionCode;
import br.com.openfinance.consents.domain.port.out.ConsentRepository;
import br.com.openfinance.consents.domain.usecase.CreateConsentUseCase;
import br.com.openfinance.consents.domain.usecase.SyncConsentStatusUseCase;
import br.com.openfinance.consents.domain.usecase.UpdateConsentStatusUseCase;
import br.com.openfinance.consents.dto.*;
import br.com.openfinance.consents.infrastructure.adapter.mapper.ConsentOpenApiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private final CreateConsentUseCase createConsentUseCase;
    private final UpdateConsentStatusUseCase updateConsentStatusUseCase;
    private final SyncConsentStatusUseCase syncConsentStatusUseCase;
    private final ConsentRepository consentRepository;
    private final ConsentOpenApiMapper mapper;
    private final IdempotencyService idempotencyService;
    private final AuthTokenProvider tokenProvider;
    private final ConsentExtensionService extensionService;

    public Mono<ResponseConsent> createConsent(CreateConsent request, String clientId, String idempotencyKey) {
        return idempotencyService.checkIdempotency(idempotencyKey, ResponseConsent.class)
                .switchIfEmpty(
                        Mono.defer(() -> {
                            // Get organisation ID from request
                            String organisationId = extractOrganisationId(clientId);

                            return tokenProvider.getToken(clientId, organisationId)
                                    .flatMap(token -> {
                                        CreateConsentCommand command = mapper.toCreateCommand(request);
                                        command.setClientId(clientId);
                                        command.setOrganisationId(organisationId);
                                        command.setToken(token);

                                        return createConsentUseCase.execute(command);
                                    })
                                    .map(mapper::toResponseConsent)
                                    .flatMap(response -> idempotencyService.saveIdempotency(
                                            idempotencyKey, response, ResponseConsent.class));
                        })
                );
    }

    public Mono<ResponseConsentRead> getConsent(String consentId, String clientId) {
        return consentRepository.findByIdAndClientId(consentId, clientId)
                .map(mapper::toResponseConsentRead);
    }

    public Mono<Void> revokeConsent(String consentId, String clientId) {
        return consentRepository.findByIdAndClientId(consentId, clientId)
                .switchIfEmpty(Mono.error(new ConsentNotFoundException("Consent not found")))
                .flatMap(consent -> {
                    if (consent.getStatus() == ConsentStatus.REJECTED) {
                        return Mono.error(new ConsentAlreadyRejectedException(
                                "CONSENTIMENTO_EM_STATUS_REJEITADO"));
                    }

                    ConsentRejectionReason reason = ConsentRejectionReason.builder()
                            .code(RejectionCode.CUSTOMER_MANUALLY_REVOKED)
                            .additionalInformation("Revoked by customer via API")
                            .build();

                    return updateConsentStatusUseCase.execute(consentId, ConsentStatus.REJECTED, reason);
                })
                .then();
    }

    public Mono<ResponseConsentExtensions> extendConsent(String consentId,
                                                         CreateConsentExtensions request,
                                                         String userId,
                                                         String ipAddress,
                                                         String userAgent) {
        ExtendConsentCommand command = ExtendConsentCommand.builder()
                .consentId(consentId)
                .expirationDateTime(mapper.toLocalDateTime(request.getData().getExpirationDateTime()))
                .loggedUserId(request.getData().getLoggedUser().getDocument().getIdentification())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        return extensionService.extendConsent(command)
                .map(this::toResponseConsentExtensions);
    }

    public Mono<ResponseConsentReadExtensions> getConsentExtensions(String consentId,
                                                                    String clientId,
                                                                    Integer page,
                                                                    Integer pageSize) {
        return extensionService.getConsentExtensions(consentId, clientId, page, pageSize)
                .map(extensions -> {
                    ResponseConsentReadExtensions response = new ResponseConsentReadExtensions();
                    response.setData(extensions.getData());
                    response.setLinks(buildLinks(consentId, page, extensions.getTotalPages()));
                    response.setMeta(buildMetaExtensions(extensions.getTotalRecords(),
                            extensions.getTotalPages()));
                    return response;
                });
    }

    private String extractOrganisationId(String clientId) {
        // Logic to extract organisation ID from client configuration
        // This is a placeholder implementation
        return "organisation-id";
    }

    private ResponseConsentExtensions toResponseConsentExtensions(ConsentExtension extension) {
        ResponseConsentExtensions response = new ResponseConsentExtensions();
        ResponseConsentExtensionsData data = new ResponseConsentExtensionsData();

        data.setConsentId(extension.getConsentId());
        data.setCreationDateTime(extension.getCreationDateTime().atOffset(ZoneOffset.UTC));
        data.setStatus(ResponseConsentExtensionsData.StatusEnum.fromValue(
                extension.getStatus().toString()));
        data.setStatusUpdateDateTime(extension.getStatusUpdateDateTime().atOffset(ZoneOffset.UTC));
        data.setPermissions(extension.getPermissions());
        data.setExpirationDateTime(extension.getExpirationDateTime() != null ?
                extension.getExpirationDateTime().atOffset(ZoneOffset.UTC) : null);

        response.setData(data);
        response.setLinks(buildLinksConsents(extension.getConsentId()));
        response.setMeta(buildMeta());

        return response;
    }

    private Links buildLinks(String consentId, Integer currentPage, Integer totalPages) {
        Links links = new Links();
        String baseUrl = "/consents/v3/consents/" + consentId + "/extensions";

        links.setSelf(baseUrl + "?page=" + currentPage);

        if (currentPage > 1) {
            links.setFirst(baseUrl + "?page=1");
            links.setPrev(baseUrl + "?page=" + (currentPage - 1));
        }

        if (currentPage < totalPages) {
            links.setNext(baseUrl + "?page=" + (currentPage + 1));
            links.setLast(baseUrl + "?page=" + totalPages);
        }

        return links;
    }

    private LinksConsents buildLinksConsents(String consentId) {
        LinksConsents links = new LinksConsents();
        links.setSelf("/consents/v3/consents/" + consentId);
        return links;
    }

    private Meta buildMeta() {
        Meta meta = new Meta();
        meta.setRequestDateTime(OffsetDateTime.now(ZoneOffset.UTC));
        return meta;
    }

    private MetaExtensions buildMetaExtensions(Long totalRecords, Integer totalPages) {
        MetaExtensions meta = new MetaExtensions();
        meta.setTotalRecords(totalRecords != null ? totalRecords.intValue() : 0);
        meta.setTotalPages(totalPages != null ? totalPages : 0);
        meta.setRequestDateTime(OffsetDateTime.now(ZoneOffset.UTC));
        return meta;
    }
}

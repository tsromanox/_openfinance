package br.com.openfinance.consents.application.service;

import br.com.openfinance.consents.domain.command.ExtendConsentCommand;
import br.com.openfinance.consents.domain.entity.*;
import br.com.openfinance.consents.domain.exception.*;
import br.com.openfinance.consents.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentExtensionService {

    private final ConsentRepository consentRepository;
    private final ConsentExtensionRepository extensionRepository;
    private final ConsentEventRepository eventRepository;
    private final ConsentEventPublisher eventPublisher;
    private final OpenFinanceApiClient apiClient;
    private final AuthTokenProvider tokenProvider;

    private static final int MAX_EXTENSION_DAYS = 365;

    public Mono<ConsentExtension> extendConsent(ExtendConsentCommand command) {
        return consentRepository.findById(command.getConsentId())
                .switchIfEmpty(Mono.error(new ConsentNotFoundException("Consent not found")))
                .flatMap(consent -> validateExtension(consent, command))
                .flatMap(consent -> processExtension(consent, command))
                .flatMap(this::saveExtension)
                .flatMap(this::publishExtensionEvent)
                .doOnSuccess(extension -> log.info("Consent {} extended successfully",
                        extension.getConsentId()))
                .doOnError(error -> log.error("Error extending consent", error));
    }

    private Mono<Consent> validateExtension(Consent consent, ExtendConsentCommand command) {
        return Mono.fromCallable(() -> {
            // Check if consent is active
            if (consent.getStatus() != ConsentStatus.AUTHORISED) {
                throw new InvalidConsentStateException("ESTADO_CONSENTIMENTO_INVALIDO",
                        "Estado inválido do consentimento",
                        "O consentimento informado não pode ser renovado sem redirecionamento " +
                                "porque está em um estado que não permite a renovação.");
            }

            // Check if consent requires multiple approval
            if (consent.isMultipleApprovalRequired()) {
                throw new MultipleApprovalRequiredException("DEPENDE_MULTIPLA_ALCADA",
                        "Necessário aprovação de múltipla alçada",
                        "O consentimento informado não pode ser renovado sem redirecionamento " +
                                "porque depende de múltipla alçada para aprovação.");
            }

            // Validate new expiration date
            if (command.getExpirationDateTime() != null) {
                validateExpirationDate(consent, command.getExpirationDateTime());
            }

            // Validate logged user
            if (!isUserAuthorizedToExtend(consent, command.getLoggedUserId())) {
                throw new UnauthorizedUserException("User not authorized to extend this consent");
            }

            return consent;
        });
    }

    private void validateExpirationDate(Consent consent, LocalDateTime newExpirationDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxDate = now.plusDays(MAX_EXTENSION_DAYS);

        if (newExpirationDate.isBefore(now)) {
            throw new InvalidExpirationDateException("DATA_EXPIRACAO_INVALIDA",
                    "Nova data para expiração do consentimento é inválida",
                    "A data de expiração deve ser futura.");
        }

        if (newExpirationDate.isAfter(maxDate)) {
            throw new InvalidExpirationDateException("DATA_EXPIRACAO_INVALIDA",
                    "Nova data para expiração do consentimento é inválida",
                    "O consentimento informado não pode ser renovado pois a nova data de " +
                            "expiração não segue a convenção do ecossistema.");
        }

        // If consent already has expiration, new date must be after current
        if (consent.getExpirationDateTime() != null &&
                newExpirationDate.isBefore(consent.getExpirationDateTime())) {
            throw new InvalidExpirationDateException("DATA_EXPIRACAO_INVALIDA",
                    "Nova data para expiração do consentimento é inválida",
                    "A nova data de expiração deve ser posterior à data atual.");
        }
    }

    private boolean isUserAuthorizedToExtend(Consent consent, String userId) {
        // For PJ consents, any authorized user can extend
        if (consent.getBusinessEntityId() != null) {
            return consent.getAuthorizedUsers().contains(userId);
        }

        // For PF consents, only the logged user who created can extend
        return consent.getLoggedUserId().equals(userId);
    }

    private Mono<ConsentExtension> processExtension(Consent consent, ExtendConsentCommand command) {
        // Create extension request for bank API
        CreateConsentExtensions apiRequest = buildExtensionRequest(command);

        return tokenProvider.getToken(consent.getClientId(), consent.getOrganisationId())
                .flatMap(token -> apiClient.extendConsent(
                        consent.getOrganisationId(),
                        consent.getConsentId(),
                        apiRequest,
                        token))
                .map(response -> buildConsentExtension(consent, command, response))
                .onErrorMap(error -> new ConsentExtensionFailedException(
                        "Failed to extend consent at bank", error));
    }

    private CreateConsentExtensions buildExtensionRequest(ExtendConsentCommand command) {
        CreateConsentExtensions request = new CreateConsentExtensions();
        CreateConsentExtensionsData data = new CreateConsentExtensionsData();

        if (command.getExpirationDateTime() != null) {
            data.setExpirationDateTime(command.getExpirationDateTime().atOffset(ZoneOffset.UTC));
        }

        LoggedUserExtensions loggedUser = new LoggedUserExtensions();
        LoggedUserDocumentExtensions document = new LoggedUserDocumentExtensions();
        document.setIdentification(command.getLoggedUserId());
        document.setRel("CPF");
        loggedUser.setDocument(document);
        data.setLoggedUser(loggedUser);

        request.setData(data);
        return request;
    }

    private ConsentExtension buildConsentExtension(Consent consent,
                                                   ExtendConsentCommand command,
                                                   ResponseConsentExtensions response) {
        return ConsentExtension.builder()
                .id(UUID.randomUUID().toString())
                .consentId(consent.getConsentId())
                .previousExpirationDateTime(consent.getExpirationDateTime())
                .newExpirationDateTime(command.getExpirationDateTime())
                .requestDateTime(LocalDateTime.now())
                .loggedUserId(command.getLoggedUserId())
                .ipAddress(command.getIpAddress())
                .userAgent(command.getUserAgent())
                .status(ConsentStatus.AUTHORISED)
                .build();
    }

    private Mono<ConsentExtension> saveExtension(ConsentExtension extension) {
        return extensionRepository.save(extension)
                .flatMap(saved -> {
                    // Update consent with new expiration
                    return consentRepository.findById(extension.getConsentId())
                            .flatMap(consent -> {
                                consent.setExpirationDateTime(extension.getNewExpirationDateTime());
                                consent.setStatusUpdateDateTime(LocalDateTime.now());
                                return consentRepository.save(consent);
                            })
                            .thenReturn(saved);
                });
    }

    private Mono<ConsentExtension> publishExtensionEvent(ConsentExtension extension) {
        ConsentEvent event = ConsentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .consentId(extension.getConsentId())
                .type(ConsentEventType.CONSENT_EXTENDED)
                .timestamp(LocalDateTime.now())
                .userId(extension.getLoggedUserId())
                .details("Consent extended until " + extension.getNewExpirationDateTime())
                .build();

        return eventRepository.save(event)
                .then(eventPublisher.publishConsentEvent(event))
                .thenReturn(extension);
    }

    public Mono<ConsentExtensionListResponse> getConsentExtensions(String consentId,
                                                                   String clientId,
                                                                   Integer page,
                                                                   Integer pageSize) {
        // Validate consent belongs to client
        return consentRepository.findByIdAndClientId(consentId, clientId)
                .switchIfEmpty(Mono.error(new ConsentNotFoundException("Consent not found")))
                .flatMap(consent -> extensionRepository.findByConsentId(consentId))
                .collectList()
                .map(extensions -> buildExtensionListResponse(extensions, page, pageSize));
    }

    private ConsentExtensionListResponse buildExtensionListResponse(List<ConsentExtension> extensions,
                                                                    Integer page,
                                                                    Integer pageSize) {
        // Sort by request date descending
        List<ConsentExtension> sorted = extensions.stream()
                .sorted(Comparator.comparing(ConsentExtension::getRequestDateTime).reversed())
                .collect(Collectors.toList());

        // Calculate pagination
        int totalRecords = sorted.size();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, totalRecords);

        // Get page data
        List<ResponseConsentReadExtensionsDataInner> data = sorted.subList(start, end).stream()
                .map(this::toResponseExtensionData)
                .collect(Collectors.toList());

        return ConsentExtensionListResponse.builder()
                .data(data)
                .totalRecords((long) totalRecords)
                .totalPages(totalPages)
                .build();
    }

    private ResponseConsentReadExtensionsDataInner toResponseExtensionData(ConsentExtension extension) {
        ResponseConsentReadExtensionsDataInner data = new ResponseConsentReadExtensionsDataInner();

        if (extension.getNewExpirationDateTime() != null) {
            data.setExpirationDateTime(extension.getNewExpirationDateTime().atOffset(ZoneOffset.UTC));
        }

        LoggedUserExtensions loggedUser = new LoggedUserExtensions();
        LoggedUserDocumentExtensions document = new LoggedUserDocumentExtensions();
        document.setIdentification(extension.getLoggedUserId());
        document.setRel("CPF");
        loggedUser.setDocument(document);
        data.setLoggedUser(loggedUser);

        data.setRequestDateTime(extension.getRequestDateTime().atOffset(ZoneOffset.UTC));

        if (extension.getPreviousExpirationDateTime() != null) {
            data.setPreviousExpirationDateTime(
                    extension.getPreviousExpirationDateTime().atOffset(ZoneOffset.UTC));
        }

        data.setXFapiCustomerIpAddress(extension.getIpAddress());
        data.setXCustomerUserAgent(extension.getUserAgent());

        return data;
    }
}

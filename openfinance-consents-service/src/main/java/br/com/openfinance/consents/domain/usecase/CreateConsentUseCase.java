package br.com.openfinance.consents.domain.usecase;

import br.com.openfinance.consents.domain.entity.*;
import br.com.openfinance.consents.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateConsentUseCase {

    private final ConsentRepository consentRepository;
    private final ConsentEventRepository eventRepository;
    private final ConsentEventPublisher eventPublisher;
    private final OpenFinanceApiClient apiClient;

    public Mono<Consent> execute(CreateConsentCommand command) {
        return validateCommand(command)
                .flatMap(this::checkConsentLimits)
                .flatMap(this::createConsentAtBank)
                .flatMap(this::saveConsent)
                .flatMap(this::publishCreationEvent)
                .doOnSuccess(consent -> log.info("Consent created successfully: {}", consent.getConsentId()))
                .doOnError(error -> log.error("Error creating consent", error));
    }

    private Mono<CreateConsentCommand> validateCommand(CreateConsentCommand command) {
        return Mono.fromCallable(() -> {
            if (command.getPermissions().isEmpty()) {
                throw new IllegalArgumentException("At least one permission is required");
            }
            if (command.getExpirationDateTime().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Expiration date must be in the future");
            }
            return command;
        });
    }

    private Mono<CreateConsentCommand> checkConsentLimits(CreateConsentCommand command) {
        return consentRepository
                .countByClientIdAndStatus(command.getClientId(), ConsentStatus.AUTHORISED)
                .flatMap(count -> {
                    if (count >= 100) { // Max 100 active consents per client
                        return Mono.error(new ConsentLimitExceededException("Client has reached maximum number of active consents"));
                    }
                    return Mono.just(command);
                });
    }

    private Mono<Consent> createConsentAtBank(CreateConsentCommand command) {
        ConsentRequest request = buildConsentRequest(command);

        return apiClient.createConsent(command.getOrganisationId(), request, command.getToken())
                .map(response -> buildConsent(command, response));
    }

    private Mono<Consent> saveConsent(Consent consent) {
        return consentRepository.save(consent);
    }

    private Mono<Consent> publishCreationEvent(Consent consent) {
        ConsentEvent event = ConsentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .consentId(consent.getConsentId())
                .type(ConsentEventType.CONSENT_CREATED)
                .timestamp(LocalDateTime.now())
                .userId(consent.getLoggedUserId())
                .newStatus(consent.getStatus())
                .build();

        return eventRepository.save(event)
                .then(eventPublisher.publishConsentEvent(event))
                .thenReturn(consent);
    }

    private ConsentRequest buildConsentRequest(CreateConsentCommand command) {
        // Build request object for API
        return ConsentRequest.builder()
                .data(ConsentRequestData.builder()
                        .loggedUser(ConsentRequestLoggedUser.builder()
                                .document(ConsentRequestDocument.builder()
                                        .identification(command.getLoggedUserDocument())
                                        .rel(command.getLoggedUserDocumentRel())
                                        .build())
                                .build())
                        .businessEntity(command.getBusinessEntityDocument() != null ?
                                ConsentRequestBusinessEntity.builder()
                                        .document(ConsentRequestDocument.builder()
                                                .identification(command.getBusinessEntityDocument())
                                                .rel(command.getBusinessEntityDocumentRel())
                                                .build())
                                        .build() : null)
                        .permissions(command.getPermissions())
                        .expirationDateTime(command.getExpirationDateTime())
                        .transactionFromDateTime(command.getTransactionFromDateTime())
                        .transactionToDateTime(command.getTransactionToDateTime())
                        .build())
                .build();
    }

    private Consent buildConsent(CreateConsentCommand command, ConsentResponse response) {
        return Consent.builder()
                .consentId(response.getData().getConsentId())
                .clientId(command.getClientId())
                .organisationId(command.getOrganisationId())
                .status(ConsentStatus.valueOf(response.getData().getStatus()))
                .creationDateTime(response.getData().getCreationDateTime())
                .statusUpdateDateTime(response.getData().getStatusUpdateDateTime())
                .expirationDateTime(response.getData().getExpirationDateTime())
                .permissions(mapPermissions(response.getData().getPermissions()))
                .loggedUserId(command.getLoggedUserDocument())
                .businessEntityId(command.getBusinessEntityDocument())
                .transactionFromDateTime(command.getTransactionFromDateTime())
                .transactionToDateTime(command.getTransactionToDateTime())
                .build();
    }
}

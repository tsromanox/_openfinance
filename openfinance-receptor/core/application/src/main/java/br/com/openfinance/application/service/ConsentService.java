package br.com.openfinance.application.service;

import br.com.openfinance.application.dto.ConsentRequest;
import br.com.openfinance.application.exception.ConsentNotFoundException;
import br.com.openfinance.application.port.input.ConsentUseCase;
import br.com.openfinance.application.port.output.ConsentRepository;
import br.com.openfinance.application.port.output.OpenFinanceClient;
import br.com.openfinance.application.port.output.ProcessingQueueRepository;
import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.domain.consent.ConsentStatus;
import br.com.openfinance.domain.consent.Permission;
import br.com.openfinance.domain.processing.JobStatus;
import br.com.openfinance.domain.processing.ProcessingJob;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConsentService implements ConsentUseCase {

    private final ConsentRepository consentRepository;
    private final ProcessingQueueRepository queueRepository;
    private final OpenFinanceClient openFinanceClient;

    public ConsentService(
            ConsentRepository consentRepository,
            ProcessingQueueRepository queueRepository,
            OpenFinanceClient openFinanceClient) {
        this.consentRepository = consentRepository;
        this.queueRepository = queueRepository;
        this.openFinanceClient = openFinanceClient;
    }

    @Override
    public Consent createConsent(CreateConsentCommand command) {
        // Criar consentimento local
        var consent = Consent.builder()
                .id(UUID.randomUUID())
                .consentId(UUID.randomUUID().toString()) // Generated consent ID for external reference
                .organizationId(command.organizationId())
                .customerId(command.customerId())
                .permissions(mapPermissions(command.permissions()))
                .status(ConsentStatus.AWAITING_AUTHORISATION)
                .createdAt(LocalDateTime.now())
                .expirationDateTime(command.expirationDateTime())
                .build();

        // Salvar no banco
        consent = consentRepository.save(consent);

        // Criar job de processamento
        var job = ProcessingJob.builder()
                .consentId(consent.getId())
                .organizationId(consent.getOrganizationId())
                .status(JobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        queueRepository.save(job);

        return consent;
    }

    @Override
    public Consent getConsent(UUID consentId) {
        return consentRepository.findById(consentId)
                .orElseThrow(() -> new ConsentNotFoundException(consentId));
    }

    @Override
    public void processConsent(UUID consentId) {
        consentRepository.findById(consentId)
                .ifPresent(consent -> {
                    try {
                        // Buscar status na instituição
                        var response = openFinanceClient.getConsent(
                                consent.getOrganizationId(),
                                consent.getConsentId()
                        );

                        // Atualizar status local
                        consentRepository.updateStatus(
                                consentId,
                                mapStatus(response.getStatus())
                        );

                        // Se autorizado, iniciar sincronização
                        if (response.getStatus().equals("AUTHORISED")) {
                            syncData(consent);
                        }
                    } catch (Exception e) {
                        handleError(consentId, e);
                    }
                });
    }

    @Override
    public void revokeConsent(UUID consentId) {
        consentRepository.updateStatus(consentId, ConsentStatus.REJECTED);
    }
    
    private Set<Permission> mapPermissions(Set<String> permissions) {
        return permissions.stream()
                .map(this::mapPermission)
                .collect(Collectors.toSet());
    }
    
    private Permission mapPermission(String permission) {
        return switch (permission) {
            case "ACCOUNTS_READ" -> Permission.ACCOUNTS_READ;
            case "ACCOUNTS_BALANCES_READ" -> Permission.ACCOUNTS_BALANCES_READ;
            case "ACCOUNTS_TRANSACTIONS_READ" -> Permission.ACCOUNTS_TRANSACTIONS_READ;
            case "ACCOUNTS_OVERDRAFT_LIMITS_READ" -> Permission.ACCOUNTS_OVERDRAFT_LIMITS_READ;
            case "CREDIT_CARDS_ACCOUNTS_READ" -> Permission.CREDIT_CARDS_ACCOUNTS_READ;
            case "CREDIT_CARDS_ACCOUNTS_LIMITS_READ" -> Permission.CREDIT_CARDS_ACCOUNTS_LIMITS_READ;
            case "CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ" -> Permission.CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ;
            case "CREDIT_CARDS_ACCOUNTS_BILLS_READ" -> Permission.CREDIT_CARDS_ACCOUNTS_BILLS_READ;
            case "RESOURCES_READ" -> Permission.RESOURCES_READ;
            default -> throw new IllegalArgumentException("Unknown permission: " + permission);
        };
    }
    
    private ConsentStatus mapStatus(String status) {
        return switch (status) {
            case "AWAITING_AUTHORISATION" -> ConsentStatus.AWAITING_AUTHORISATION;
            case "AUTHORISED" -> ConsentStatus.AUTHORISED;
            case "REJECTED" -> ConsentStatus.REJECTED;
            case "CONSUMED" -> ConsentStatus.CONSUMED;
            case "EXPIRED" -> ConsentStatus.EXPIRED;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }
    
    private void syncData(Consent consent) {
        // Criar jobs para sincronização de dados
        var syncJob = ProcessingJob.builder()
                .consentId(consent.getId())
                .organizationId(consent.getOrganizationId())
                .status(JobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        queueRepository.save(syncJob);
    }
    
    private void handleError(UUID consentId, Exception e) {
        // Log do erro e possível reprocessamento
        System.err.println("Error processing consent " + consentId + ": " + e.getMessage());
        // Aqui poderia implementar estratégia de retry ou mover para dead letter queue
    }
}

package br.com.openfinance.consents.service;

import br.com.openfinance.consents.entity.ConsentEntity;
import br.com.openfinance.consents.entity.ConsentResourceEntity;
import br.com.openfinance.consents.mapper.ConsentMapper;
import br.com.openfinance.consents.model.*;
import br.com.openfinance.consents.repository.ConsentRepository;
import br.com.openfinance.consents.repository.ConsentResourceRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service principal para gerenciamento de consentimentos
 * Utiliza Virtual Threads para processamento paralelo massivo
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Observed(name = "consents.service")
public class ConsentsService {

    private final ConsentRepository consentRepository;
    private final ConsentResourceRepository resourceRepository;
    private final ConsentMapper consentMapper;
    private final MongoTemplate mongoTemplate;
    private final Executor virtualThreadExecutor;

    /**
     * Busca consentimentos com paginação e filtros
     * Processa consultas em paralelo usando Virtual Threads
     */
    @Timed(value = "consents.service.get.all")
    @Transactional(readOnly = true)
    public ResponseConsents getConsents(
            Integer page, Integer pageSize, String status,
            OffsetDateTime fromDate, OffsetDateTime toDate,
            UUID interactionId) {
        
        log.info("Fetching consents - Page: {}, Size: {}, Status: {}, InteractionId: {}", 
                page, pageSize, status, interactionId);
        
        try {
            // Criar query com filtros
            Query query = buildConsentQuery(status, fromDate, toDate);
            PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "creationDateTime"));
            
            // Executar contagem e busca em paralelo com Virtual Threads
            CompletableFuture<Long> countFuture = CompletableFuture.supplyAsync(
                () -> mongoTemplate.count(query, ConsentEntity.class),
                virtualThreadExecutor
            );
            
            CompletableFuture<List<ConsentEntity>> consentsFuture = CompletableFuture.supplyAsync(
                () -> mongoTemplate.find(query.with(pageRequest), ConsentEntity.class),
                virtualThreadExecutor
            );
            
            // Aguardar resultados
            Long totalRecords = countFuture.get(5, TimeUnit.SECONDS);
            List<ConsentEntity> entities = consentsFuture.get(5, TimeUnit.SECONDS);
            
            // Processar recursos em paralelo para cada consentimento
            List<Consent> consents = processConsentsInParallel(entities);
            
            // Construir resposta
            return buildConsentsResponse(consents, page, pageSize, totalRecords.intValue());
            
        } catch (Exception e) {
            log.error("Error fetching consents", e);
            throw new RuntimeException("Failed to fetch consents", e);
        }
    }

    /**
     * Busca consentimento por ID com cache
     */
    @Cacheable(value = "consents", key = "#consentId")
    @Timed(value = "consents.service.get.by.id")
    @Transactional(readOnly = true)
    public ResponseConsent getConsentById(String consentId, UUID interactionId) {
        log.info("Fetching consent by ID: {}, InteractionId: {}", consentId, interactionId);
        
        ConsentEntity entity = consentRepository.findById(consentId)
            .orElseThrow(() -> new NoSuchElementException("Consent not found: " + consentId));
        
        Consent consent = consentMapper.toModel(entity);
        
        return buildConsentResponse(consent);
    }

    /**
     * Busca recursos do consentimento
     * Executa queries paralelas para diferentes tipos de recursos
     */
    @Timed(value = "consents.service.get.resources")
    @Transactional(readOnly = true)
    public ResponseConsentResources getConsentResources(String consentId, UUID interactionId) {
        log.info("Fetching consent resources for ID: {}, InteractionId: {}", consentId, interactionId);
        
        // Verificar se consentimento existe
        if (!consentRepository.existsById(consentId)) {
            throw new NoSuchElementException("Consent not found: " + consentId);
        }
        
        try {
            // Buscar recursos de diferentes tipos em paralelo
            List<CompletableFuture<List<ConsentResourceEntity>>> futures = Arrays.asList(
                CompletableFuture.supplyAsync(() -> 
                    resourceRepository.findByConsentIdAndType(consentId, "ACCOUNT"), virtualThreadExecutor),
                CompletableFuture.supplyAsync(() -> 
                    resourceRepository.findByConsentIdAndType(consentId, "CREDIT_CARD_ACCOUNT"), virtualThreadExecutor),
                CompletableFuture.supplyAsync(() -> 
                    resourceRepository.findByConsentIdAndType(consentId, "LOAN"), virtualThreadExecutor),
                CompletableFuture.supplyAsync(() -> 
                    resourceRepository.findByConsentIdAndType(consentId, "INVESTMENT"), virtualThreadExecutor)
            );
            
            // Aguardar todos os resultados
            List<ConsentResource> resources = futures.stream()
                .map(future -> {
                    try {
                        return future.get(3, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.warn("Error fetching resources", e);
                        return new ArrayList<ConsentResourceEntity>();
                    }
                })
                .flatMap(List::stream)
                .map(consentMapper::toResourceModel)
                .collect(Collectors.toList());
            
            return buildResourcesResponse(resources);
            
        } catch (Exception e) {
            log.error("Error fetching consent resources", e);
            throw new RuntimeException("Failed to fetch consent resources", e);
        }
    }

    /**
     * Processa consentimentos em paralelo usando Virtual Threads
     */
    private List<Consent> processConsentsInParallel(List<ConsentEntity> entities) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Consent>> futures = entities.stream()
                .map(entity -> executor.submit(() -> {
                    Consent consent = consentMapper.toModel(entity);
                    // Enriquecer com dados adicionais se necessário
                    enrichConsentData(consent);
                    return consent;
                }))
                .collect(Collectors.toList());
            
            return futures.stream()
                .map(future -> {
                    try {
                        return future.get(1, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.warn("Error processing consent", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
    }

    /**
     * Enriquece dados do consentimento (exemplo: validações adicionais)
     */
    private void enrichConsentData(Consent consent) {
        // Validar expiração
        if (consent.getExpirationDateTime() != null && 
            consent.getExpirationDateTime().isBefore(OffsetDateTime.now())) {
            consent.setStatus(Consent.StatusEnum.CONSUMED);
        }
    }

    /**
     * Constrói query MongoDB com filtros
     */
    private Query buildConsentQuery(String status, OffsetDateTime fromDate, OffsetDateTime toDate) {
        Query query = new Query();
        
        if (status != null && !status.isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        
        if (fromDate != null) {
            query.addCriteria(Criteria.where("creationDateTime").gte(fromDate));
        }
        
        if (toDate != null) {
            query.addCriteria(Criteria.where("creationDateTime").lte(toDate));
        }
        
        return query;
    }

    /**
     * Constrói resposta de lista de consentimentos
     */
    private ResponseConsents buildConsentsResponse(List<Consent> consents, Integer page, Integer pageSize, Integer totalRecords) {
        ResponseConsents response = new ResponseConsents();
        response.setData(consents);
        
        Links links = new Links();
        links.setSelf("/open-banking/consents/v3/consents?page=" + page + "&page-size=" + pageSize);
        if (page > 1) {
            links.setPrev("/open-banking/consents/v3/consents?page=" + (page - 1) + "&page-size=" + pageSize);
        }
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        if (page < totalPages) {
            links.setNext("/open-banking/consents/v3/consents?page=" + (page + 1) + "&page-size=" + pageSize);
        }
        response.setLinks(links);
        
        Meta meta = new Meta();
        meta.setTotalRecords(totalRecords);
        meta.setTotalPages(totalPages);
        meta.setRequestDateTime(OffsetDateTime.now());
        response.setMeta(meta);
        
        return response;
    }

    /**
     * Constrói resposta de consentimento único
     */
    private ResponseConsent buildConsentResponse(Consent consent) {
        ResponseConsent response = new ResponseConsent();
        response.setData(consent);
        
        Links links = new Links();
        links.setSelf("/open-banking/consents/v3/consents/" + consent.getConsentId());
        response.setLinks(links);
        
        MetaSingle meta = new MetaSingle();
        meta.setRequestDateTime(OffsetDateTime.now());
        response.setMeta(meta);
        
        return response;
    }

    /**
     * Constrói resposta de recursos
     */
    private ResponseConsentResources buildResourcesResponse(List<ConsentResource> resources) {
        ResponseConsentResources response = new ResponseConsentResources();
        response.setData(resources);
        
        Links links = new Links();
        response.setLinks(links);
        
        Meta meta = new Meta();
        meta.setTotalRecords(resources.size());
        meta.setTotalPages(1);
        meta.setRequestDateTime(OffsetDateTime.now());
        response.setMeta(meta);
        
        return response;
    }

    /**
     * Processar múltiplos consentimentos em batch usando Virtual Threads
     * Útil para processamento em massa
     */
    @Async("virtualThreadExecutor")
    public CompletableFuture<List<Consent>> processBatchConsents(List<String> consentIds) {
        log.info("Processing batch of {} consents", consentIds.size());
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<Consent>> subtasks = consentIds.stream()
                .map(id -> scope.fork(() -> {
                    ConsentEntity entity = consentRepository.findById(id).orElse(null);
                    return entity != null ? consentMapper.toModel(entity) : null;
                }))
                .toList();
            
            scope.join();
            scope.throwIfFailed();
            
            return CompletableFuture.completedFuture(
                subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
            );
        } catch (Exception e) {
            log.error("Error processing batch consents", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
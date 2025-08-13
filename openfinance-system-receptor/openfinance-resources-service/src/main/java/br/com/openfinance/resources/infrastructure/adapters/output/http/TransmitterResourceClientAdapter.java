package br.com.openfinance.resources.infrastructure.adapters.output.http;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceType;
import br.com.openfinance.resources.domain.ports.output.TransmitterResourceClient;
import br.com.openfinance.resources.infrastructure.adapters.output.http.dto.ResourceListResponse;
import br.com.openfinance.resources.infrastructure.adapters.output.http.dto.ResourceResponse;
import br.com.openfinance.resources.infrastructure.adapters.output.http.mapper.TransmitterResponseMapper;
import br.com.openfinance.core.infrastructure.monitoring.Monitored;
import br.com.openfinance.core.domain.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransmitterResourceClientAdapter implements TransmitterResourceClient {

    private final RestTemplate restTemplate;
    private final TransmitterResponseMapper responseMapper;

    @Value("${openfinance.transmitter.base-url:https://api.transmitter.com}")
    private String baseUrl;

    @Value("${openfinance.transmitter.timeout:30000}")
    private int timeout;

    @Override
    @Monitored
    public Resource fetchResource(String participantId, String externalResourceId) {
        log.debug("Fetching resource {} from participant: {}", externalResourceId, participantId);
        
        try {
            String url = String.format("%s/open-banking/resources/v1/resources/%s", 
                    baseUrl, externalResourceId);
            
            HttpHeaders headers = createHeaders(participantId);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ResourceResponse> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    ResourceResponse.class
            );
            
            if (response.getBody() == null) {
                throw new BusinessException("Empty response from transmitter", "EMPTY_RESPONSE");
            }
            
            // Note: In a real implementation, you would need to get customerId and brandId
            // from the context or another source
            Resource resource = responseMapper.toDomain(
                    response.getBody(), 
                    participantId, 
                    "unknown-customer", // This should come from context
                    "unknown-brand"     // This should come from context
            );
            
            log.debug("Successfully fetched resource: {}", externalResourceId);
            return resource;
            
        } catch (RestClientException e) {
            log.error("Error fetching resource {} from participant {}: {}", 
                    externalResourceId, participantId, e.getMessage());
            throw new BusinessException("Failed to fetch resource from transmitter", "FETCH_ERROR", e);
        }
    }

    @Override
    @Monitored
    public List<Resource> fetchResourcesByCustomer(String participantId, String customerId) {
        log.debug("Fetching resources for customer {} from participant: {}", customerId, participantId);
        
        try {
            String url = String.format("%s/open-banking/resources/v1/resources", baseUrl);
            
            HttpHeaders headers = createHeaders(participantId);
            headers.add("x-customer-user-agent", customerId);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ResourceListResponse> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    ResourceListResponse.class
            );
            
            if (response.getBody() == null || response.getBody().getData() == null) {
                return new ArrayList<>();
            }
            
            List<Resource> resources = response.getBody().getData().stream()
                    .map(resourceResponse -> responseMapper.toDomain(
                            resourceResponse, 
                            participantId, 
                            customerId, 
                            "unknown-brand"
                    ))
                    .collect(Collectors.toList());
            
            log.debug("Successfully fetched {} resources for customer: {}", resources.size(), customerId);
            return resources;
            
        } catch (RestClientException e) {
            log.error("Error fetching resources for customer {} from participant {}: {}", 
                    customerId, participantId, e.getMessage());
            throw new BusinessException("Failed to fetch resources from transmitter", "FETCH_ERROR", e);
        }
    }

    @Override
    @Monitored
    public List<Resource> fetchResourcesByType(String participantId, ResourceType type) {
        log.debug("Fetching resources by type {} from participant: {}", type, participantId);
        
        try {
            String url = String.format("%s/open-banking/resources/v1/resources?type=%s", 
                    baseUrl, type.getCode());
            
            HttpHeaders headers = createHeaders(participantId);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ResourceListResponse> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    ResourceListResponse.class
            );
            
            if (response.getBody() == null || response.getBody().getData() == null) {
                return new ArrayList<>();
            }
            
            List<Resource> resources = response.getBody().getData().stream()
                    .map(resourceResponse -> responseMapper.toDomain(
                            resourceResponse, 
                            participantId, 
                            "unknown-customer", 
                            "unknown-brand"
                    ))
                    .collect(Collectors.toList());
            
            log.debug("Successfully fetched {} resources by type: {}", resources.size(), type);
            return resources;
            
        } catch (RestClientException e) {
            log.error("Error fetching resources by type {} from participant {}: {}", 
                    type, participantId, e.getMessage());
            throw new BusinessException("Failed to fetch resources from transmitter", "FETCH_ERROR", e);
        }
    }

    @Override
    @Monitored
    public Resource fetchResourceDetails(String participantId, String externalResourceId) {
        log.debug("Fetching resource details for {} from participant: {}", externalResourceId, participantId);
        
        try {
            String url = String.format("%s/open-banking/resources/v1/resources/%s/details", 
                    baseUrl, externalResourceId);
            
            HttpHeaders headers = createHeaders(participantId);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<ResourceResponse> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    ResourceResponse.class
            );
            
            if (response.getBody() == null) {
                throw new BusinessException("Empty response from transmitter", "EMPTY_RESPONSE");
            }
            
            Resource resource = responseMapper.toDomain(
                    response.getBody(), 
                    participantId, 
                    "unknown-customer", 
                    "unknown-brand"
            );
            
            log.debug("Successfully fetched resource details: {}", externalResourceId);
            return resource;
            
        } catch (RestClientException e) {
            log.error("Error fetching resource details {} from participant {}: {}", 
                    externalResourceId, participantId, e.getMessage());
            throw new BusinessException("Failed to fetch resource details from transmitter", "FETCH_ERROR", e);
        }
    }

    private HttpHeaders createHeaders(String participantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + getAccessToken(participantId));
        headers.add("x-fapi-auth-date", String.valueOf(System.currentTimeMillis()));
        headers.add("x-fapi-customer-ip-address", "127.0.0.1");
        headers.add("x-fapi-interaction-id", java.util.UUID.randomUUID().toString());
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        return headers;
    }

    private String getAccessToken(String participantId) {
        // In a real implementation, this would:
        // 1. Check if we have a valid cached token
        // 2. If not, perform OAuth2 client credentials flow
        // 3. Cache the token with expiration
        return "mock-access-token";
    }
}
package br.com.openfinance.application.port.output;

import br.com.openfinance.service.resources.domain.Resource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for resource discovery operations.
 * Handles communication with external Open Finance Brasil directories
 * and resource providers for discovery operations.
 */
public interface ResourceDiscoveryService {
    
    /**
     * Discover resources from Open Finance Brasil directory endpoint.
     */
    CompletableFuture<List<Resource>> discoverFromEndpoint(String discoveryEndpoint);
    
    /**
     * Discover resources from multiple endpoints in parallel.
     */
    CompletableFuture<List<Resource>> discoverFromEndpoints(List<String> discoveryEndpoints);
    
    /**
     * Validate discovery endpoint availability.
     */
    CompletableFuture<Boolean> validateDiscoveryEndpoint(String endpoint);
    
    /**
     * Get list of known discovery endpoints.
     */
    List<String> getKnownDiscoveryEndpoints();
    
    /**
     * Refresh discovery endpoints from central registry.
     */
    CompletableFuture<List<String>> refreshDiscoveryEndpoints();
}
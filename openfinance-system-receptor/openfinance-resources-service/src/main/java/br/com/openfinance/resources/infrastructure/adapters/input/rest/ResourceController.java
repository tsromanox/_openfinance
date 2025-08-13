package br.com.openfinance.resources.infrastructure.adapters.input.rest;

import br.com.openfinance.resources.application.dto.CreateResourceRequest;
import br.com.openfinance.resources.application.dto.ResourceDTO;
import br.com.openfinance.resources.application.mapper.ResourceMapper;
import br.com.openfinance.resources.application.services.ResourceApplicationService;
import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceType;
import br.com.openfinance.core.infrastructure.monitoring.Monitored;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/open-banking/resources/v1")
@Slf4j
@RequiredArgsConstructor
@Validated
public class ResourceController {

    private final ResourceApplicationService resourceService;
    private final ResourceMapper resourceMapper;

    @GetMapping("/resources/{resourceId}")
    @Monitored
    public ResponseEntity<ResourceDTO> getResource(@PathVariable UUID resourceId) {
        log.info("GET /resources/{}", resourceId);
        
        Optional<Resource> resource = resourceService.getResource(resourceId);
        
        return resource
                .map(r -> {
                    ResourceDTO dto = resourceMapper.toDTO(r);
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/resources")
    @Monitored
    public ResponseEntity<Page<ResourceDTO>> listResources(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String participantId,
            @RequestParam(required = false) ResourceType type,
            @PageableDefault(size = 25, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.info("GET /resources with customerId={}, participantId={}, type={}", customerId, participantId, type);
        
        Page<Resource> resources;
        
        if (customerId != null && type != null) {
            resources = resourceService.listResourcesByCustomerAndType(customerId, type, pageable);
        } else if (customerId != null) {
            resources = resourceService.listResourcesByCustomer(customerId, pageable);
        } else if (participantId != null) {
            resources = resourceService.listResourcesByParticipant(participantId, pageable);
        } else if (type != null) {
            resources = resourceService.listResourcesByType(type, pageable);
        } else {
            return ResponseEntity.badRequest().build();
        }
        
        Page<ResourceDTO> dtoPage = resources.map(resourceMapper::toDTO);
        
        return ResponseEntity.ok(dtoPage);
    }

    @PostMapping("/resources")
    @Monitored
    public ResponseEntity<ResourceDTO> createResource(@Valid @RequestBody CreateResourceRequest request) {
        log.info("POST /resources for customer: {}", request.getCustomerId());
        
        Resource resource = resourceMapper.toEntity(request);
        Resource savedResource = resourceService.createResource(resource);
        ResourceDTO dto = resourceMapper.toDTO(savedResource);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/resources/{resourceId}")
    @Monitored
    public ResponseEntity<ResourceDTO> updateResource(
            @PathVariable UUID resourceId, 
            @Valid @RequestBody ResourceDTO resourceDTO) {
        
        log.info("PUT /resources/{}", resourceId);
        
        // Verify resource exists
        Optional<Resource> existingResource = resourceService.getResource(resourceId);
        if (existingResource.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        resourceDTO.setResourceId(resourceId);
        Resource resource = resourceMapper.toEntity(resourceDTO);
        Resource updatedResource = resourceService.updateResource(resource);
        ResourceDTO dto = resourceMapper.toDTO(updatedResource);
        
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/resources/{resourceId}/sync")
    @Monitored
    public ResponseEntity<ResourceDTO> syncResource(@PathVariable UUID resourceId) {
        log.info("POST /resources/{}/sync", resourceId);
        
        try {
            Resource syncedResource = resourceService.syncResource(resourceId);
            ResourceDTO dto = resourceMapper.toDTO(syncedResource);
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error syncing resource {}: {}", resourceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/resources/sync/customer/{customerId}")
    @Monitored
    public ResponseEntity<Void> syncResourcesByCustomer(@PathVariable String customerId) {
        log.info("POST /resources/sync/customer/{}", customerId);
        
        try {
            resourceService.syncResourcesByCustomer(customerId);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("Error syncing resources for customer {}: {}", customerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/resources/sync/all")
    @Monitored
    public ResponseEntity<Void> syncAllResources() {
        log.info("POST /resources/sync/all");
        
        try {
            resourceService.syncAllResources();
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            log.error("Error syncing all resources: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/resources/external/{participantId}/{externalResourceId}")
    @Monitored
    public ResponseEntity<ResourceDTO> getResourceByExternalId(
            @PathVariable String participantId,
            @PathVariable String externalResourceId) {
        
        log.info("GET /resources/external/{}/{}", participantId, externalResourceId);
        
        Optional<Resource> resource = resourceService.getResourceByExternalId(participantId, externalResourceId);
        
        return resource
                .map(r -> {
                    ResourceDTO dto = resourceMapper.toDTO(r);
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Resources Service is healthy");
    }
}
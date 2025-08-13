# Core Application Module

## Overview

The `core-application` module implements the application layer following hexagonal architecture principles. It contains use cases, ports (interfaces), application services, and DTOs that orchestrate domain operations. This layer acts as a bridge between the domain logic and external infrastructure.

## Architecture

```
core-application/
├── src/main/java/br/com/openfinance/application/
│   ├── dto/               # Data Transfer Objects
│   ├── exception/         # Application-specific exceptions
│   ├── port/
│   │   ├── input/         # Use case interfaces (input ports)
│   │   └── output/        # Repository and service interfaces (output ports)
│   └── service/           # Application service implementations
```

## Hexagonal Architecture Implementation

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           External Actors                                      │
│               (REST Controllers, Message Consumers, etc.)                      │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │
                               Input Ports
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         Application Layer                                      │
│                    (Use Cases & Application Services)                          │
└─────────────────────────────────────────────────────────────────────────────────┘
                                      │
                               Output Ports
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         Infrastructure Layer                                   │
│              (Repositories, External APIs, Message Publishers)                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

## Input Ports (Use Cases)

Input ports define the business operations that the application can perform. They represent the primary entry points into the application layer.

### AccountUseCase
**Location**: `br.com.openfinance.application.port.input.AccountUseCase`

Defines operations for account management and synchronization.

```java
public interface AccountUseCase {
    /**
     * Synchronize accounts for a specific consent.
     * Retrieves account data from external APIs and updates local storage.
     */
    List<Account> syncAccountsForConsent(UUID consentId);
    
    /**
     * Get detailed account information including balance and metadata.
     */
    Account getAccountDetails(String accountId);
    
    /**
     * Update account balance from external source.
     */
    void updateAccountBalance(String accountId);
}
```

**Business Operations**:
- **Account Synchronization**: Batch sync of accounts for authorized consents
- **Account Retrieval**: Get individual account with full details
- **Balance Updates**: Real-time balance synchronization

### ConsentUseCase
**Location**: `br.com.openfinance.application.port.input.ConsentUseCase`

Manages consent lifecycle and permission validation.

```java
public interface ConsentUseCase {
    /**
     * Create a new consent with specified permissions.
     */
    Consent createConsent(CreateConsentCommand command);
    
    /**
     * Retrieve consent by ID with full details.
     */
    Consent getConsent(UUID consentId);
    
    /**
     * Process consent authorization and initiate data access.
     */
    void processConsent(UUID consentId);
    
    /**
     * Revoke consent and cleanup associated data.
     */
    void revokeConsent(UUID consentId);
    
    /**
     * Command for creating new consents.
     */
    record CreateConsentCommand(
            String organizationId,
            String customerId,
            Set<String> permissions,
            LocalDateTime expirationDateTime
    ) {}
}
```

**Business Operations**:
- **Consent Creation**: Create new consent with permissions and expiration
- **Consent Processing**: Handle authorization workflow
- **Consent Revocation**: Cleanup and data removal
- **Permission Validation**: Check access rights for specific operations

### ResourceUseCase
**Location**: `br.com.openfinance.application.port.input.ResourceUseCase`

High-performance resource management with Virtual Threads support.

```java
public interface ResourceUseCase {
    /**
     * Discover resources from Open Finance Brasil directory endpoints.
     */
    CompletableFuture<List<Resource>> discoverResources(List<String> discoveryEndpoints);
    
    /**
     * Synchronize resource data from Open Finance institutions.
     */
    CompletableFuture<ResourceSyncResult> synchronizeResources(List<String> resourceIds);
    
    /**
     * Validate resource endpoints and data quality.
     */
    CompletableFuture<ResourceValidationResult> validateResources(List<String> resourceIds);
    
    /**
     * Monitor resource health and availability.
     */
    CompletableFuture<ResourceHealthResult> monitorResourceHealth(List<String> resourceIds);
    
    // Synchronous operations for direct access
    Resource getResource(String resourceId);
    Resource updateResourceStatus(String resourceId, ResourceStatus newStatus);
    List<Resource> getResourcesByType(ResourceType type, ResourceStatus status);
    List<Resource> getResourcesByOrganization(String organizationId);
    
    // Batch operations for maintenance
    List<Resource> getResourcesNeedingSync();
    List<Resource> getResourcesNeedingValidation();
    List<Resource> getResourcesNeedingMonitoring();
}
```

**Business Operations**:
- **Resource Discovery**: Parallel discovery from multiple endpoints
- **Resource Synchronization**: Batch sync with adaptive performance
- **Resource Validation**: Quality checks and compliance validation
- **Resource Monitoring**: Health checks and availability monitoring

## Output Ports

Output ports define interfaces for external dependencies. Infrastructure modules implement these interfaces.

### Repository Ports

#### ConsentRepository
**Location**: `br.com.openfinance.application.port.output.ConsentRepository`

```java
public interface ConsentRepository {
    Consent save(Consent consent);
    Optional<Consent> findById(UUID id);
    List<Consent> findByOrganizationId(String organizationId);
    List<Consent> findByStatus(ConsentStatus status);
    void deleteById(UUID id);
}
```

#### ResourceRepository
**Location**: `br.com.openfinance.application.port.output.ResourceRepository`

```java
public interface ResourceRepository {
    // Basic CRUD operations
    Resource save(Resource resource);
    List<Resource> saveAll(List<Resource> resources);
    Optional<Resource> findById(String resourceId);
    
    // Query operations
    List<Resource> findByOrganizationId(String organizationId);
    List<Resource> findByTypeAndStatus(ResourceType type, ResourceStatus status);
    List<Resource> findByStatus(ResourceStatus status);
    List<Resource> findAll();
    
    // Maintenance operations
    List<Resource> findResourcesNeedingSync(LocalDateTime threshold);
    List<Resource> findResourcesNeedingValidation(LocalDateTime threshold);
    List<Resource> findResourcesNeedingMonitoring(LocalDateTime threshold);
    
    // Batch operations
    long countByStatus(ResourceStatus status);
    long countByType(ResourceType type);
    void updateStatus(String resourceId, ResourceStatus status);
    void updateLastSyncAt(String resourceId, LocalDateTime lastSyncAt);
}
```

### External Service Ports

#### OpenFinanceClient
**Location**: `br.com.openfinance.application.port.output.OpenFinanceClient`

Interface for integrating with Open Finance Brasil APIs.

```java
public interface OpenFinanceClient {
    /**
     * Create consent with external institution.
     */
    ConsentResponse createConsent(String orgId, ConsentRequest request);
    
    /**
     * Get consent status from external institution.
     */
    ConsentResponse getConsent(String orgId, String consentId);
    
    /**
     * Retrieve accounts for authorized consent.
     */
    AccountsResponse getAccounts(String orgId, String token);
    
    /**
     * Get account balance information.
     */
    BalanceResponse getBalance(String orgId, String accountId, String token);
}
```

#### ResourceDiscoveryService
**Location**: `br.com.openfinance.application.port.output.ResourceDiscoveryService`

Service for discovering resources from Open Finance Brasil directories.

```java
public interface ResourceDiscoveryService {
    /**
     * Discover resources from single endpoint.
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
```

#### ProcessingQueueRepository
**Location**: `br.com.openfinance.application.port.output.ProcessingQueueRepository`

Interface for managing background job processing.

```java
public interface ProcessingQueueRepository {
    ProcessingJob save(ProcessingJob job);
    Optional<ProcessingJob> findById(UUID id);
    List<ProcessingJob> findPendingJobs();
    List<ProcessingJob> findJobsByStatus(JobStatus status);
    void updateJobStatus(UUID jobId, JobStatus status);
    void incrementRetryCount(UUID jobId);
    void markJobCompleted(UUID jobId, String result);
    void markJobFailed(UUID jobId, String errorMessage);
}
```

## Application Services

Application services implement the use cases by orchestrating domain entities and coordinating with infrastructure through output ports.

### AccountService
**Location**: `br.com.openfinance.application.service.AccountService`

```java
@Service
@Transactional
public class AccountService implements AccountUseCase {
    
    private final ConsentRepository consentRepository;
    private final OpenFinanceClient openFinanceClient;
    
    @Override
    public List<Account> syncAccountsForConsent(UUID consentId) {
        // 1. Validate consent exists and is active
        Consent consent = consentRepository.findById(consentId)
            .orElseThrow(() -> new ConsentNotFoundException(consentId));
            
        if (!consent.canAccessAccounts()) {
            throw new ConsentNotAuthorizedException(consentId, "ACCOUNTS_READ");
        }
        
        // 2. Fetch accounts from external API
        AccountsResponse response = openFinanceClient.getAccounts(
            consent.getOrganizationId(), 
            generateAccessToken(consent)
        );
        
        // 3. Convert and save accounts
        List<Account> accounts = response.getData().stream()
            .map(dto -> mapToAccount(dto, consentId))
            .toList();
            
        // 4. Update sync timestamp and return
        return accounts.stream()
            .map(account -> account.withUpdatedBalance(
                syncAccountBalance(account.getAccountId())
            ))
            .toList();
    }
    
    // Additional methods...
}
```

### ConsentService
**Location**: `br.com.openfinance.application.service.ConsentService`

```java
@Service
@Transactional
public class ConsentService implements ConsentUseCase {
    
    private final ConsentRepository consentRepository;
    private final OpenFinanceClient openFinanceClient;
    private final ProcessingQueueRepository queueRepository;
    
    @Override
    public Consent createConsent(CreateConsentCommand command) {
        // 1. Validate command
        validateCreateConsentCommand(command);
        
        // 2. Create domain entity
        Consent consent = ConsentFactory.createConsent(
            command.organizationId(),
            command.customerId(),
            command.permissions(),
            command.expirationDateTime()
        );
        
        // 3. Save locally
        Consent savedConsent = consentRepository.save(consent);
        
        // 4. Create external consent
        ConsentRequest request = mapToConsentRequest(savedConsent);
        ConsentResponse response = openFinanceClient.createConsent(
            command.organizationId(), 
            request
        );
        
        // 5. Update with external consent ID
        Consent updatedConsent = savedConsent.toBuilder()
            .consentId(response.getConsentId())
            .build();
            
        return consentRepository.save(updatedConsent);
    }
    
    // Additional methods...
}
```

### ResourceService
**Location**: `br.com.openfinance.application.service.ResourceService`

High-performance resource service with Virtual Thread integration.

```java
@Service
@Transactional
public class ResourceService implements ResourceUseCase {
    
    private final ResourceRepository resourceRepository;
    private final ResourceDiscoveryService resourceDiscoveryService;
    private final VirtualThreadResourceService virtualThreadResourceService;
    
    @Override
    public CompletableFuture<List<Resource>> discoverResources(List<String> discoveryEndpoints) {
        log.info("Starting resource discovery from {} endpoints", discoveryEndpoints.size());
        
        return virtualThreadResourceService.discoverResourcesAsync(discoveryEndpoints)
                .thenApply(resources -> {
                    // Save discovered resources
                    List<Resource> savedResources = resourceRepository.saveAll(resources);
                    
                    log.info("Successfully discovered and saved {} resources", savedResources.size());
                    return savedResources;
                })
                .exceptionally(e -> {
                    log.error("Resource discovery failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Resource discovery failed", e);
                });
    }
    
    @Override
    public CompletableFuture<ResourceSyncResult> synchronizeResources(List<String> resourceIds) {
        return virtualThreadResourceService.syncResourcesAsync(resourceIds)
                .thenApply(syncResult -> {
                    // Update sync timestamps for successful resources
                    resourceIds.forEach(resourceId -> {
                        resourceRepository.updateLastSyncAt(resourceId, LocalDateTime.now());
                    });
                    
                    return mapToResourceSyncResult(syncResult);
                });
    }
    
    // Additional async methods...
}
```

## Data Transfer Objects (DTOs)

DTOs are used for data transfer between layers and external systems.

### Request DTOs

#### ConsentRequest
**Location**: `br.com.openfinance.application.dto.ConsentRequest`

```java
@Builder
public record ConsentRequest(
    @NotNull String organizationId,
    @NotNull String customerId,
    @NotEmpty Set<String> permissions,
    LocalDateTime expirationDateTime,
    Map<String, Object> additionalData
) {}
```

### Response DTOs

#### ConsentResponse
**Location**: `br.com.openfinance.application.dto.ConsentResponse`

```java
@Builder
public record ConsentResponse(
    String consentId,
    ConsentStatus status,
    Set<String> permissions,
    LocalDateTime createdAt,
    LocalDateTime expirationDateTime,
    String statusReason
) {}
```

#### AccountsResponse
**Location**: `br.com.openfinance.application.dto.AccountsResponse`

```java
@Builder
public record AccountsResponse(
    List<AccountData> data,
    Links links,
    Meta meta
) {
    @Builder
    public record AccountData(
        String accountId,
        String brandName,
        String companyCnpj,
        String type,
        String subtype,
        String number,
        String checkDigit,
        String agencyNumber,
        String agencyCheckDigit
    ) {}
}
```

#### ResourceResponse
**Location**: `br.com.openfinance.application.dto.ResourceResponse`

```java
public class ResourceResponse {
    
    public record ResourceInfo(
        String resourceId,
        String organizationId,
        String organizationName,
        String cnpj,
        ResourceType type,
        ResourceStatus status,
        LocalDateTime discoveredAt,
        LocalDateTime lastSyncedAt,
        LocalDateTime lastValidatedAt,
        LocalDateTime lastMonitoredAt
    ) {}
    
    public record ResourceDiscoveryResponse(
        int resourceCount,
        long durationMs,
        String strategy,
        boolean success,
        String message,
        List<ResourceInfo> resources
    ) {}
    
    // Additional response types...
}
```

## Application Exceptions

Application-specific exceptions that represent business error conditions.

### AccountNotFoundException
**Location**: `br.com.openfinance.application.exception.AccountNotFoundException`

```java
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }
}
```

### ConsentNotAuthorizedException
**Location**: `br.com.openfinance.application.exception.ConsentNotAuthorizedException`

```java
public class ConsentNotAuthorizedException extends RuntimeException {
    public ConsentNotAuthorizedException(UUID consentId, String permission) {
        super(String.format("Consent %s not authorized for permission: %s", consentId, permission));
    }
}
```

### ResourceNotFoundException
**Location**: `br.com.openfinance.application.exception.ResourceNotFoundException`

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceId, String resourceType) {
        super(String.format("Resource of type '%s' with ID '%s' not found", resourceType, resourceId));
    }
}
```

## Transaction Management

The application layer manages transactions using Spring's `@Transactional` annotation:

```java
@Service
@Transactional
public class ConsentService implements ConsentUseCase {
    
    @Transactional(readOnly = true)
    public Consent getConsent(UUID consentId) {
        // Read-only operation
    }
    
    @Transactional(rollbackFor = Exception.class)
    public Consent createConsent(CreateConsentCommand command) {
        // Write operation with explicit rollback
    }
}
```

## Validation Strategy

### Command Validation
Input validation is performed on commands and requests:

```java
private void validateCreateConsentCommand(CreateConsentCommand command) {
    if (command.organizationId() == null || command.organizationId().trim().isEmpty()) {
        throw new IllegalArgumentException("Organization ID cannot be empty");
    }
    
    if (command.permissions() == null || command.permissions().isEmpty()) {
        throw new IllegalArgumentException("At least one permission must be specified");
    }
    
    if (command.expirationDateTime() != null && 
        command.expirationDateTime().isBefore(LocalDateTime.now())) {
        throw new IllegalArgumentException("Expiration date cannot be in the past");
    }
}
```

### Domain Validation
Domain validation is delegated to domain entities:

```java
public Consent createConsent(CreateConsentCommand command) {
    // Domain entity validates business rules
    return Consent.builder()
        .organizationId(command.organizationId())
        .customerId(command.customerId())
        .permissions(mapPermissions(command.permissions()))
        .expirationDateTime(command.expirationDateTime())
        .build(); // Throws domain exception if invalid
}
```

## Error Handling Strategy

### Exception Mapping
Application exceptions are mapped from domain and infrastructure exceptions:

```java
@Override
public Account getAccountDetails(String accountId) {
    try {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));
    } catch (DataAccessException e) {
        log.error("Database error retrieving account {}: {}", accountId, e.getMessage());
        throw new ApplicationException("Failed to retrieve account", e);
    }
}
```

### Async Error Handling
Asynchronous operations handle errors with CompletableFuture:

```java
@Override
public CompletableFuture<List<Resource>> discoverResources(List<String> discoveryEndpoints) {
    return resourceDiscoveryService.discoverFromEndpoints(discoveryEndpoints)
        .exceptionally(throwable -> {
            log.error("Resource discovery failed", throwable);
            // Could return default value or re-throw
            throw new ResourceDiscoveryException("Discovery failed", throwable);
        });
}
```

## Testing Strategy

### Unit Testing
Test application services in isolation using mocks:

```java
@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {
    
    @Mock
    private ConsentRepository consentRepository;
    
    @Mock
    private OpenFinanceClient openFinanceClient;
    
    @InjectMocks
    private ConsentService consentService;
    
    @Test
    void shouldCreateConsentSuccessfully() {
        // Given
        CreateConsentCommand command = new CreateConsentCommand(
            "org123", "customer456", Set.of("ACCOUNTS_READ"), null);
            
        when(consentRepository.save(any())).thenReturn(mockConsent);
        when(openFinanceClient.createConsent(any(), any())).thenReturn(mockResponse);
        
        // When
        Consent result = consentService.createConsent(command);
        
        // Then
        assertThat(result).isNotNull();
        verify(consentRepository).save(any(Consent.class));
        verify(openFinanceClient).createConsent(eq("org123"), any());
    }
}
```

### Integration Testing
Test use cases with TestContainers:

```java
@SpringBootTest
@Testcontainers
class ResourceServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private ResourceUseCase resourceUseCase;
    
    @Test
    void shouldDiscoverAndSaveResources() {
        // Given
        List<String> endpoints = List.of("http://test-endpoint.com");
        
        // When
        List<Resource> resources = resourceUseCase.discoverResources(endpoints).join();
        
        // Then
        assertThat(resources).isNotEmpty();
    }
}
```

## Best Practices

### Use Case Design
- Keep use cases focused on a single business operation
- Use dependency injection for infrastructure dependencies
- Handle errors appropriately for business context
- Use transactions effectively for data consistency

### Port Design
- Define clear interfaces that express business intent
- Use domain types in port signatures, not technical types
- Keep ports focused and cohesive
- Design for testability with mock implementations

### DTO Design
- Use records for immutable data transfer
- Include validation annotations where appropriate
- Keep DTOs simple and focused on data transfer
- Use builder pattern for complex DTOs

This application layer provides a clean, testable, and maintainable way to orchestrate business operations while maintaining separation from infrastructure concerns.
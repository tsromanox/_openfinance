# Core Domain Module

## Overview

The `core-domain` module contains the pure business logic of the OpenFinance Receptor system. It implements domain entities, value objects, and business rules following Domain-Driven Design (DDD) principles. This module has no external dependencies and represents the heart of the business domain.

## Architecture

```
core-domain/
├── src/main/java/br/com/openfinance/domain/
│   ├── account/           # Account domain entities
│   ├── consent/           # Consent domain entities  
│   ├── event/             # Domain events
│   ├── exception/         # Domain exceptions
│   └── processing/        # Processing job entities
```

## Domain Entities

### Account Domain

#### Account Entity
**Location**: `br.com.openfinance.domain.account.Account`

The Account entity represents a financial account in the Open Finance Brasil context.

```java
@Builder
@Getter
public class Account {
    private final UUID id;
    private final String accountId;
    private final String brandName;
    private final String companyCnpj;
    private final String type;
    private final String subtype;
    private final String number;
    private final String checkDigit;
    private final String agencyNumber;
    private final String agencyCheckDigit;
    private final Balance balance;
    private final UUID consentId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime lastSyncAt;
}
```

**Key Business Methods**:
- `needsSync()`: Determines if account needs synchronization (> 1 hour since last sync)
- `getAccountNumber()`: Returns formatted account number with check digit
- `getAgencyNumber()`: Returns formatted agency number with check digit
- `withUpdatedBalance(Balance)`: Creates new instance with updated balance
- `isValid()`: Validates account data integrity

**Business Rules**:
- Account ID cannot be empty
- Brand name cannot be empty
- Company CNPJ must follow Brazilian CNPJ format (14 digits)
- Account and agency numbers must have valid check digits
- Consent ID is mandatory for all accounts

#### Value Objects

**AccountNumber** - `br.com.openfinance.domain.account.AccountNumber`
```java
public record AccountNumber(String number, String checkDigit) {
    // Validation and formatting logic
}
```

**AgencyNumber** - `br.com.openfinance.domain.account.AgencyNumber`  
```java
public record AgencyNumber(String number, String checkDigit) {
    // Validation and formatting logic
}
```

**Balance** - `br.com.openfinance.domain.account.Balance`
```java
@Builder
@Getter
public class Balance {
    private final BigDecimal availableAmount;
    private final BigDecimal blockedAmount;
    private final BigDecimal automaticallyInvestedAmount;
    private final String currency;
    private final LocalDateTime updatedAt;
}
```

### Consent Domain

#### Consent Entity
**Location**: `br.com.openfinance.domain.consent.Consent`

The Consent entity manages authorization permissions for accessing financial data.

```java
public class Consent {
    private final UUID id;
    private final String consentId;
    private final String organizationId;
    private final String customerId;
    private final ConsentStatus status;
    private final Set<Permission> permissions;
    private final LocalDateTime createdAt;
    private final LocalDateTime expirationDateTime;
}
```

**Key Business Methods**:
- `isExpired()`: Checks if consent has passed expiration date
- `isActive()`: Validates consent is authorized and not expired
- `canAccessAccounts()`: Checks if consent allows account access
- `canAccessBalances()`: Checks if consent allows balance access
- `canAccessResources()`: Checks if consent allows resource access
- `hasPermission(Permission)`: Validates specific permission
- `withStatus(ConsentStatus)`: Creates new instance with updated status

**Business Rules**:
- Organization ID and Customer ID cannot be empty
- Expiration date cannot be before creation date
- Active consent must be AUTHORISED status and not expired
- Specific permissions required for different data access types

#### Enumerations

**ConsentStatus** - `br.com.openfinance.domain.consent.ConsentStatus`
```java
public enum ConsentStatus {
    AWAITING_AUTHORISATION,
    AUTHORISED,
    REJECTED,
    CONSUMED,
    REVOKED
}
```

**Permission** - `br.com.openfinance.domain.consent.Permission`
```java
public enum Permission {
    // Account permissions
    ACCOUNTS_READ("accounts", "Read account information"),
    ACCOUNTS_BALANCES_READ("accounts", "Read account balances"),
    ACCOUNTS_TRANSACTIONS_READ("accounts", "Read account transactions"),
    
    // Credit card permissions
    CREDIT_CARDS_ACCOUNTS_READ("credit-cards", "Read credit card account information"),
    
    // Resources permissions  
    RESOURCES_READ("resources", "Read resources information"),
    
    // Loans and financing permissions
    LOANS_READ("loans", "Read loans information"),
    FINANCINGS_READ("financings", "Read financings information");
}
```

**Business Methods**:
- `isAccountPermission()`: Check if permission is account-related
- `isCreditCardPermission()`: Check if permission is credit card-related
- `isResourcePermission()`: Check if permission is resource-related
- `isLoanPermission()`: Check if permission is loan-related
- `isFinancingPermission()`: Check if permission is financing-related

### Processing Domain

#### ProcessingJob Entity
**Location**: `br.com.openfinance.domain.processing.ProcessingJob`

Represents background jobs for data processing and synchronization.

```java
@Builder
@Getter
public class ProcessingJob {
    private final UUID id;
    private final String jobType;
    private final String entityId;
    private final JobStatus status;
    private final String payload;
    private final Integer retryCount;
    private final Integer maxRetries;
    private final LocalDateTime createdAt;
    private final LocalDateTime scheduledAt;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final String errorMessage;
}
```

**JobStatus** - `br.com.openfinance.domain.processing.JobStatus`
```java
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

## Domain Events

### Event Base Class
**Location**: `br.com.openfinance.domain.event.DomainEvent`

```java
public interface DomainEvent {
    UUID getEventId();
    LocalDateTime getOccurredAt();
    String getEventType();
    Map<String, Object> getEventData();
}
```

### Specific Events

**ConsentCreatedEvent** - `br.com.openfinance.domain.event.ConsentCreatedEvent`
- Triggered when a new consent is created
- Contains consent ID, organization ID, customer ID, and permissions

**ConsentStatusChangedEvent** - `br.com.openfinance.domain.event.ConsentStatusChangedEvent`
- Triggered when consent status changes
- Contains old status, new status, and reason for change

**AccountSyncedEvent** - `br.com.openfinance.domain.event.AccountSyncedEvent`
- Triggered when account data is synchronized
- Contains account ID, sync timestamp, and updated fields

## Domain Exceptions

### Base Exception
**Location**: `br.com.openfinance.domain.exception.DomainException`

```java
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
    
    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### Specific Exceptions

**InvalidAccountException** - `br.com.openfinance.domain.exception.InvalidAccountException`
- Thrown when account data validation fails
- Contains specific validation error details

**InvalidConsentException** - `br.com.openfinance.domain.exception.InvalidConsentException`  
- Thrown when consent data validation fails
- Contains consent ID and validation error details

## Design Patterns Used

### Builder Pattern
All complex entities use the Builder pattern for construction:

```java
Account account = Account.builder()
    .accountId("12345")
    .brandName("Bank Name")
    .companyCnpj("12345678901234")
    .type("CONTA_DEPOSITO_A_VISTA")
    .number("1234567")
    .checkDigit("0")
    .agencyNumber("1234")
    .consentId(consentId)
    .createdAt(LocalDateTime.now())
    .build();
```

### Immutable Objects
All entities are immutable, with methods returning new instances for changes:

```java
Account updatedAccount = account.withUpdatedBalance(newBalance);
Consent revokedConsent = consent.withStatus(ConsentStatus.REVOKED);
```

### Value Objects
Complex values are represented as value objects:

```java
AccountNumber accountNumber = new AccountNumber("1234567", "0");
Balance balance = Balance.builder()
    .availableAmount(new BigDecimal("1000.00"))
    .currency("BRL")
    .build();
```

## Validation Rules

### Account Validation
- **Account ID**: Not null, not empty
- **Brand Name**: Not null, not empty
- **Company CNPJ**: 14 digits, valid format
- **Account Number**: Valid check digit algorithm
- **Agency Number**: Valid check digit algorithm
- **Consent ID**: Not null, must reference existing consent

### Consent Validation
- **Organization ID**: Not null, not empty
- **Customer ID**: Not null, not empty
- **Expiration Date**: Cannot be before creation date
- **Permissions**: At least one permission required
- **Status Transitions**: Must follow valid state machine

### Processing Job Validation
- **Job Type**: Not null, not empty
- **Entity ID**: Not null, must reference existing entity
- **Max Retries**: Must be >= 0
- **Payload**: Valid JSON format when present

## Integration Points

### With Core Application
The domain module provides entities and business rules that are used by:
- **Use Cases**: Application services operate on domain entities
- **Repositories**: Persistence layer stores and retrieves domain entities
- **Event Publishers**: Domain events are published to application layer

### With Service Modules
Service modules use domain entities for:
- **Business Logic**: Service-specific operations on domain entities
- **Validation**: Service-level validation using domain validation methods
- **Event Processing**: Handling domain events for service-specific actions

## Testing Strategy

### Unit Testing
- **Entity Behavior**: Test business methods and validation rules
- **Value Object Validation**: Test construction and validation logic
- **Domain Events**: Test event creation and data integrity
- **Exception Handling**: Test domain exception scenarios

### Example Test
```java
@Test
void shouldRequireSyncWhenLastSyncOlderThanOneHour() {
    // Given
    LocalDateTime oldSyncTime = LocalDateTime.now().minusHours(2);
    Account account = Account.builder()
        .accountId("12345")
        .lastSyncAt(oldSyncTime)
        // ... other required fields
        .build();
    
    // When
    boolean needsSync = account.needsSync();
    
    // Then
    assertTrue(needsSync);
}
```

## Best Practices

### Domain Entity Design
- Keep entities focused on business behavior
- Avoid anemic domain model - include business logic in entities
- Use immutable objects for thread safety
- Validate business rules in entity constructors and methods

### Value Object Design
- Use records for simple value objects
- Include validation in construction
- Make value objects comparable and hashable
- Use value objects to make implicit concepts explicit

### Domain Event Design
- Events should represent business-significant occurrences
- Include all necessary data in event payload
- Use past tense for event names (CreatedEvent, UpdatedEvent)
- Keep events immutable and serializable

This domain module provides a solid foundation for the OpenFinance Receptor system by encapsulating core business concepts and rules in a clean, testable, and maintainable way.
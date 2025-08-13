package br.com.openfinance.service.consents.validation;

import br.com.openfinance.application.port.output.ConsentRepository;
import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.domain.consent.ConsentStatus;
import br.com.openfinance.domain.consent.Permission;
import br.com.openfinance.service.consents.VirtualThreadConsentService;
import br.com.openfinance.service.consents.monitoring.ConsentPerformanceMonitor;
import br.com.openfinance.service.consents.resource.AdaptiveConsentResourceManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;

/**
 * Advanced parallel consent validator using Virtual Threads and Structured Concurrency
 * for high-performance validation operations.
 */
@Slf4j
@Component
public class ParallelConsentValidator {

    private final ConsentRepository consentRepository;
    private final TaskExecutor validationExecutor;
    private final AdaptiveConsentResourceManager resourceManager;
    private final ConsentPerformanceMonitor performanceMonitor;
    private final MeterRegistry meterRegistry;

    // Validation configuration
    @Value("${openfinance.consents.validation.timeout.seconds:30}")
    private long validationTimeoutSeconds;

    @Value("${openfinance.consents.validation.parallel.enabled:true}")
    private boolean parallelValidationEnabled;

    @Value("${openfinance.consents.validation.business-rules.enabled:true}")
    private boolean businessRulesValidationEnabled;

    @Value("${openfinance.consents.validation.external.enabled:true}")
    private boolean externalValidationEnabled;

    public ParallelConsentValidator(
            ConsentRepository consentRepository,
            @Qualifier("consentValidationExecutor") TaskExecutor validationExecutor,
            AdaptiveConsentResourceManager resourceManager,
            ConsentPerformanceMonitor performanceMonitor,
            MeterRegistry meterRegistry) {

        this.consentRepository = consentRepository;
        this.validationExecutor = validationExecutor;
        this.resourceManager = resourceManager;
        this.performanceMonitor = performanceMonitor;
        this.meterRegistry = meterRegistry;

        log.info("ParallelConsentValidator initialized with parallel validation: {}", parallelValidationEnabled);
    }

    /**
     * Validate consent business rules using parallel processing.
     */
    public VirtualThreadConsentService.ConsentBusinessValidationResult validateBusinessRules(UUID consentId) {
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try {
            Consent consent = consentRepository.findById(consentId)
                    .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + consentId));

            if (!businessRulesValidationEnabled) {
                return new VirtualThreadConsentService.ConsentBusinessValidationResult(
                        consentId, true, List.of(), List.of("Business rules validation disabled")
                );
            }

            if (parallelValidationEnabled) {
                return validateBusinessRulesInParallel(consent, sample);
            } else {
                return validateBusinessRulesSequentially(consent, sample);
            }

        } catch (Exception e) {
            sample.stop(Timer.builder("consent.validation.business.rules.duration")
                    .tag("status", "error")
                    .register(meterRegistry));

            log.error("Error validating business rules for consent {}: {}", consentId, e.getMessage(), e);
            performanceMonitor.recordError("business_validation_error", "VALIDATE_BUSINESS_RULES", false);
            
            return new VirtualThreadConsentService.ConsentBusinessValidationResult(
                    consentId, false, List.of("Validation failed: " + e.getMessage()), List.of()
            );
        }
    }

    /**
     * Validate multiple consents concurrently using Virtual Threads.
     */
    public CompletableFuture<List<VirtualThreadConsentService.ConsentBusinessValidationResult>> 
            validateMultipleConsentsAsync(List<UUID> consentIds) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Validating {} consents using Virtual Threads", consentIds.size());

                List<CompletableFuture<VirtualThreadConsentService.ConsentBusinessValidationResult>> validationFutures = 
                        consentIds.stream()
                        .map(consentId -> CompletableFuture.supplyAsync(() -> validateBusinessRules(consentId), validationExecutor))
                        .toList();

                // Wait for all validations to complete
                CompletableFuture.allOf(validationFutures.toArray(new CompletableFuture[0])).get();

                // Collect results
                List<VirtualThreadConsentService.ConsentBusinessValidationResult> results = validationFutures.stream()
                        .map(CompletableFuture::join)
                        .toList();

                long validCount = results.stream().mapToLong(result -> result.valid() ? 1 : 0).sum();
                log.debug("Batch validation completed: {} valid out of {}", validCount, results.size());

                return results;

            } catch (Exception e) {
                log.error("Error in batch consent validation", e);
                performanceMonitor.recordError("batch_validation_error", "VALIDATE_MULTIPLE_CONSENTS", true);
                throw new RuntimeException("Batch validation failed", e);
            }
        }, validationExecutor);
    }

    /**
     * Validate consent permissions using Structured Concurrency.
     */
    public ConsentPermissionValidationResult validateConsentPermissions(
            UUID consentId, Set<String> requiredPermissions) {
        
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try {
            Consent consent = consentRepository.findById(consentId)
                    .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + consentId));

            if (parallelValidationEnabled && requiredPermissions.size() > 1) {
                return validatePermissionsWithStructuredConcurrency(consent, requiredPermissions, sample);
            } else {
                return validatePermissionsSequentially(consent, requiredPermissions, sample);
            }

        } catch (Exception e) {
            sample.stop(Timer.builder("consent.validation.permissions.duration")
                    .tag("status", "error")
                    .register(meterRegistry));

            log.error("Error validating permissions for consent {}: {}", consentId, e.getMessage(), e);
            performanceMonitor.recordError("permission_validation_error", "VALIDATE_PERMISSIONS", false);
            
            return new ConsentPermissionValidationResult(
                    consentId, false, Set.of(), List.of("Permission validation failed: " + e.getMessage())
            );
        }
    }

    /**
     * Comprehensive consent validation using all available rules.
     */
    public ComprehensiveValidationResult validateConsentComprehensively(UUID consentId) {
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try {
            Consent consent = consentRepository.findById(consentId)
                    .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + consentId));

            if (parallelValidationEnabled) {
                return performComprehensiveValidationInParallel(consent, sample);
            } else {
                return performComprehensiveValidationSequentially(consent, sample);
            }

        } catch (Exception e) {
            sample.stop(Timer.builder("consent.validation.comprehensive.duration")
                    .tag("status", "error")
                    .register(meterRegistry));

            log.error("Error in comprehensive validation for consent {}: {}", consentId, e.getMessage(), e);
            performanceMonitor.recordError("comprehensive_validation_error", "VALIDATE_COMPREHENSIVE", false);
            
            return new ComprehensiveValidationResult(
                    consentId, false, List.of("Comprehensive validation failed: " + e.getMessage()), 
                    List.of(), null, null
            );
        }
    }

    // Private validation methods

    private VirtualThreadConsentService.ConsentBusinessValidationResult validateBusinessRulesInParallel(
            Consent consent, Timer.Sample sample) {
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Parallel business rule validations
            var statusValidationTask = scope.fork(() -> validateConsentStatus(consent));
            var expirationValidationTask = scope.fork(() -> validateConsentExpiration(consent));
            var permissionsValidationTask = scope.fork(() -> validateConsentPermissionsIntegrity(consent));
            var organizationValidationTask = scope.fork(() -> validateOrganizationIntegrity(consent));
            var customerValidationTask = scope.fork(() -> validateCustomerIntegrity(consent));
            
            scope.join();
            scope.throwIfFailed();
            
            // Collect validation results
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            
            addValidationResult(statusValidationTask.get(), errors, warnings);
            addValidationResult(expirationValidationTask.get(), errors, warnings);
            addValidationResult(permissionsValidationTask.get(), errors, warnings);
            addValidationResult(organizationValidationTask.get(), errors, warnings);
            addValidationResult(customerValidationTask.get(), errors, warnings);
            
            boolean isValid = errors.isEmpty();
            
            long duration = sample.stop(Timer.builder("consent.validation.business.rules.duration")
                    .tag("strategy", "parallel")
                    .tag("status", isValid ? "success" : "failure")
                    .register(meterRegistry));
            
            performanceMonitor.recordConsentOperation("VALIDATE_BUSINESS_RULES", isValid, duration);
            
            return new VirtualThreadConsentService.ConsentBusinessValidationResult(
                    consent.getId(), isValid, errors, warnings
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Business rules validation interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Business rules validation failed", e);
        }
    }

    private VirtualThreadConsentService.ConsentBusinessValidationResult validateBusinessRulesSequentially(
            Consent consent, Timer.Sample sample) {
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Sequential validation
        addValidationResult(validateConsentStatus(consent), errors, warnings);
        addValidationResult(validateConsentExpiration(consent), errors, warnings);
        addValidationResult(validateConsentPermissionsIntegrity(consent), errors, warnings);
        addValidationResult(validateOrganizationIntegrity(consent), errors, warnings);
        addValidationResult(validateCustomerIntegrity(consent), errors, warnings);
        
        boolean isValid = errors.isEmpty();
        
        long duration = sample.stop(Timer.builder("consent.validation.business.rules.duration")
                .tag("strategy", "sequential")
                .tag("status", isValid ? "success" : "failure")
                .register(meterRegistry));
        
        performanceMonitor.recordConsentOperation("VALIDATE_BUSINESS_RULES", isValid, duration);
        
        return new VirtualThreadConsentService.ConsentBusinessValidationResult(
                consent.getId(), isValid, errors, warnings
        );
    }

    private ConsentPermissionValidationResult validatePermissionsWithStructuredConcurrency(
            Consent consent, Set<String> requiredPermissions, Timer.Sample sample) {
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Parallel permission validations
            List<StructuredTaskScope.Subtask<PermissionValidationResult>> permissionTasks = 
                    requiredPermissions.stream()
                    .map(permission -> scope.fork(() -> validateSinglePermission(consent, permission)))
                    .toList();
            
            scope.join();
            scope.throwIfFailed();
            
            // Collect results
            Set<String> validPermissions = permissionTasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .filter(PermissionValidationResult::valid)
                    .map(PermissionValidationResult::permission)
                    .collect(java.util.stream.Collectors.toSet());
            
            List<String> errors = permissionTasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .filter(result -> !result.valid())
                    .map(PermissionValidationResult::errorMessage)
                    .toList();
            
            boolean allValid = validPermissions.size() == requiredPermissions.size();
            
            long duration = sample.stop(Timer.builder("consent.validation.permissions.duration")
                    .tag("strategy", "structured-concurrency")
                    .tag("status", allValid ? "success" : "partial")
                    .register(meterRegistry));
            
            performanceMonitor.recordConsentOperation("VALIDATE_PERMISSIONS", allValid, duration);
            
            return new ConsentPermissionValidationResult(
                    consent.getId(), allValid, validPermissions, errors
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Permission validation interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Permission validation failed", e);
        }
    }

    private ConsentPermissionValidationResult validatePermissionsSequentially(
            Consent consent, Set<String> requiredPermissions, Timer.Sample sample) {
        
        Set<String> validPermissions = requiredPermissions.stream()
                .filter(permission -> validateSinglePermission(consent, permission).valid())
                .collect(java.util.stream.Collectors.toSet());
        
        List<String> errors = requiredPermissions.stream()
                .filter(permission -> !validateSinglePermission(consent, permission).valid())
                .map(permission -> "Permission not granted: " + permission)
                .toList();
        
        boolean allValid = validPermissions.size() == requiredPermissions.size();
        
        long duration = sample.stop(Timer.builder("consent.validation.permissions.duration")
                .tag("strategy", "sequential")
                .tag("status", allValid ? "success" : "partial")
                .register(meterRegistry));
        
        performanceMonitor.recordConsentOperation("VALIDATE_PERMISSIONS", allValid, duration);
        
        return new ConsentPermissionValidationResult(
                consent.getId(), allValid, validPermissions, errors
        );
    }

    private ComprehensiveValidationResult performComprehensiveValidationInParallel(
            Consent consent, Timer.Sample sample) {
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Parallel comprehensive validations
            var businessRulesTask = scope.fork(() -> validateBusinessRulesInParallel(consent, Timer.Sample.start(meterRegistry)));
            var permissionsTask = scope.fork(() -> validateConsentPermissionsIntegrity(consent));
            var dataIntegrityTask = scope.fork(() -> validateDataIntegrity(consent));
            var complianceTask = scope.fork(() -> validateCompliance(consent));
            
            scope.join();
            scope.throwIfFailed();
            
            // Collect all results
            var businessRulesResult = businessRulesTask.get();
            var permissionsResult = permissionsTask.get();
            var dataIntegrityResult = dataIntegrityTask.get();
            var complianceResult = complianceTask.get();
            
            List<String> allErrors = new ArrayList<>();
            List<String> allWarnings = new ArrayList<>();
            
            allErrors.addAll(businessRulesResult.validationErrors());
            allWarnings.addAll(businessRulesResult.warnings());
            
            addValidationResult(permissionsResult, allErrors, allWarnings);
            addValidationResult(dataIntegrityResult, allErrors, allWarnings);
            addValidationResult(complianceResult, allErrors, allWarnings);
            
            boolean isValid = allErrors.isEmpty();
            
            long duration = sample.stop(Timer.builder("consent.validation.comprehensive.duration")
                    .tag("strategy", "parallel")
                    .tag("status", isValid ? "success" : "failure")
                    .register(meterRegistry));
            
            performanceMonitor.recordConsentOperation("VALIDATE_COMPREHENSIVE", isValid, duration);
            
            return new ComprehensiveValidationResult(
                    consent.getId(), isValid, allErrors, allWarnings, 
                    businessRulesResult, permissionsResult
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Comprehensive validation interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Comprehensive validation failed", e);
        }
    }

    private ComprehensiveValidationResult performComprehensiveValidationSequentially(
            Consent consent, Timer.Sample sample) {
        
        // Sequential comprehensive validation
        var businessRulesResult = validateBusinessRulesSequentially(consent, Timer.Sample.start(meterRegistry));
        var permissionsResult = validateConsentPermissionsIntegrity(consent);
        var dataIntegrityResult = validateDataIntegrity(consent);
        var complianceResult = validateCompliance(consent);
        
        List<String> allErrors = new ArrayList<>();
        List<String> allWarnings = new ArrayList<>();
        
        allErrors.addAll(businessRulesResult.validationErrors());
        allWarnings.addAll(businessRulesResult.warnings());
        
        addValidationResult(permissionsResult, allErrors, allWarnings);
        addValidationResult(dataIntegrityResult, allErrors, allWarnings);
        addValidationResult(complianceResult, allErrors, allWarnings);
        
        boolean isValid = allErrors.isEmpty();
        
        long duration = sample.stop(Timer.builder("consent.validation.comprehensive.duration")
                .tag("strategy", "sequential")
                .tag("status", isValid ? "success" : "failure")
                .register(meterRegistry));
        
        performanceMonitor.recordConsentOperation("VALIDATE_COMPREHENSIVE", isValid, duration);
        
        return new ComprehensiveValidationResult(
                consent.getId(), isValid, allErrors, allWarnings, 
                businessRulesResult, permissionsResult
        );
    }

    // Individual validation methods

    private ValidationResult validateConsentStatus(Consent consent) {
        if (consent.getStatus() == null) {
            return ValidationResult.error("Consent status is null");
        }
        
        if (consent.getStatus() == ConsentStatus.EXPIRED) {
            return ValidationResult.warning("Consent is expired");
        }
        
        return ValidationResult.success();
    }

    private ValidationResult validateConsentExpiration(Consent consent) {
        if (consent.getExpirationDateTime() == null) {
            return ValidationResult.error("Consent expiration date is null");
        }
        
        if (consent.getExpirationDateTime().isBefore(LocalDateTime.now())) {
            return ValidationResult.error("Consent has expired");
        }
        
        // Warning if expiring soon (within 24 hours)
        if (consent.getExpirationDateTime().isBefore(LocalDateTime.now().plusHours(24))) {
            return ValidationResult.warning("Consent expires within 24 hours");
        }
        
        return ValidationResult.success();
    }

    private ValidationResult validateConsentPermissionsIntegrity(Consent consent) {
        if (consent.getPermissions() == null || consent.getPermissions().isEmpty()) {
            return ValidationResult.error("Consent has no permissions");
        }
        
        // Check for invalid permission combinations
        Set<Permission> permissions = consent.getPermissions();
        if (permissions.contains(Permission.ACCOUNTS_READ) && 
            !permissions.contains(Permission.RESOURCES_READ)) {
            return ValidationResult.warning("ACCOUNTS_READ permission without RESOURCES_READ may limit functionality");
        }
        
        return ValidationResult.success();
    }

    private ValidationResult validateOrganizationIntegrity(Consent consent) {
        if (consent.getOrganizationId() == null || consent.getOrganizationId().trim().isEmpty()) {
            return ValidationResult.error("Organization ID is null or empty");
        }
        
        // Additional organization validation logic here
        return ValidationResult.success();
    }

    private ValidationResult validateCustomerIntegrity(Consent consent) {
        if (consent.getCustomerId() == null || consent.getCustomerId().trim().isEmpty()) {
            return ValidationResult.error("Customer ID is null or empty");
        }
        
        // Additional customer validation logic here
        return ValidationResult.success();
    }

    private ValidationResult validateDataIntegrity(Consent consent) {
        if (consent.getId() == null) {
            return ValidationResult.error("Consent ID is null");
        }
        
        if (consent.getConsentId() == null || consent.getConsentId().trim().isEmpty()) {
            return ValidationResult.error("External consent ID is null or empty");
        }
        
        if (consent.getCreatedAt() == null) {
            return ValidationResult.error("Creation timestamp is null");
        }
        
        return ValidationResult.success();
    }

    private ValidationResult validateCompliance(Consent consent) {
        // Open Finance Brasil compliance validation
        if (consent.getExpirationDateTime() != null && 
            consent.getCreatedAt() != null &&
            consent.getExpirationDateTime().isAfter(consent.getCreatedAt().plusMonths(12))) {
            return ValidationResult.error("Consent duration exceeds 12 months limit");
        }
        
        return ValidationResult.success();
    }

    private PermissionValidationResult validateSinglePermission(Consent consent, String requiredPermission) {
        boolean hasPermission = consent.getPermissions().stream()
                .anyMatch(permission -> permission.name().equals(requiredPermission));
        
        if (hasPermission) {
            return new PermissionValidationResult(requiredPermission, true, null);
        } else {
            return new PermissionValidationResult(requiredPermission, false, 
                    "Permission not granted: " + requiredPermission);
        }
    }

    private void addValidationResult(ValidationResult result, List<String> errors, List<String> warnings) {
        if (result.type() == ValidationResult.Type.ERROR) {
            errors.add(result.message());
        } else if (result.type() == ValidationResult.Type.WARNING) {
            warnings.add(result.message());
        }
    }

    // Result records
    public record ConsentPermissionValidationResult(
            UUID consentId,
            boolean allPermissionsValid,
            Set<String> validPermissions,
            List<String> errors
    ) {}

    public record ComprehensiveValidationResult(
            UUID consentId,
            boolean isValid,
            List<String> errors,
            List<String> warnings,
            VirtualThreadConsentService.ConsentBusinessValidationResult businessRulesResult,
            ValidationResult permissionsResult
    ) {}

    private record ValidationResult(Type type, String message) {
        enum Type { SUCCESS, WARNING, ERROR }
        
        static ValidationResult success() {
            return new ValidationResult(Type.SUCCESS, null);
        }
        
        static ValidationResult warning(String message) {
            return new ValidationResult(Type.WARNING, message);
        }
        
        static ValidationResult error(String message) {
            return new ValidationResult(Type.ERROR, message);
        }
    }

    private record PermissionValidationResult(
            String permission,
            boolean valid,
            String errorMessage
    ) {}
}
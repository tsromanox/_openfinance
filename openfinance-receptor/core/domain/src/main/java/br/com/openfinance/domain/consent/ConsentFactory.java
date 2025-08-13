package br.com.openfinance.domain.consent;

import br.com.openfinance.domain.exception.InvalidConsentException;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Factory class for creating Consent domain objects with validation.
 */
public class ConsentFactory {
    
    public static Consent createNewConsent(
            String organizationId, 
            String customerId, 
            Set<String> permissionCodes, 
            LocalDateTime expirationDateTime) {
        
        validateInputs(organizationId, customerId, permissionCodes);
        
        Set<Permission> permissions = convertPermissions(permissionCodes);
        
        return Consent.builder()
                .id(UUID.randomUUID())
                .consentId(generateConsentId())
                .organizationId(organizationId)
                .customerId(customerId)
                .permissions(permissions)
                .status(ConsentStatus.AWAITING_AUTHORISATION)
                .createdAt(LocalDateTime.now())
                .expirationDateTime(expirationDateTime)
                .build();
    }
    
    public static Consent createFromExistingData(
            UUID id,
            String consentId,
            String organizationId,
            String customerId,
            Set<Permission> permissions,
            ConsentStatus status,
            LocalDateTime createdAt,
            LocalDateTime expirationDateTime) {
        
        return Consent.builder()
                .id(id)
                .consentId(consentId)
                .organizationId(organizationId)
                .customerId(customerId)
                .permissions(permissions)
                .status(status)
                .createdAt(createdAt)
                .expirationDateTime(expirationDateTime)
                .build();
    }
    
    private static void validateInputs(String organizationId, String customerId, Set<String> permissionCodes) {
        if (organizationId == null || organizationId.trim().isEmpty()) {
            throw new InvalidConsentException("Organization ID cannot be null or empty");
        }
        
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new InvalidConsentException("Customer ID cannot be null or empty");
        }
        
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw new InvalidConsentException("At least one permission must be specified");
        }
    }
    
    private static Set<Permission> convertPermissions(Set<String> permissionCodes) {
        return permissionCodes.stream()
                .map(ConsentFactory::mapPermission)
                .collect(Collectors.toSet());
    }
    
    private static Permission mapPermission(String code) {
        try {
            return Permission.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidConsentException("Invalid permission code: " + code);
        }
    }
    
    private static String generateConsentId() {
        // Generate a consent ID following Open Finance Brasil patterns
        return "urn:bancoex:" + UUID.randomUUID().toString();
    }
}
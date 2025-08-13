package br.com.openfinance.service.resources.domain;

/**
 * Enumeration of resource operational status.
 */
public enum ResourceStatus {
    
    /**
     * Resource is newly discovered and pending initial validation.
     */
    DISCOVERED("Discovered", "Resource discovered but not yet validated"),
    
    /**
     * Resource is currently being validated.
     */
    VALIDATING("Validating", "Resource validation in progress"),
    
    /**
     * Resource is active and available for use.
     */
    ACTIVE("Active", "Resource is operational and available"),
    
    /**
     * Resource is temporarily unavailable but expected to recover.
     */
    TEMPORARILY_UNAVAILABLE("Temporarily Unavailable", "Resource temporarily offline"),
    
    /**
     * Resource is under maintenance.
     */
    MAINTENANCE("Maintenance", "Resource under scheduled maintenance"),
    
    /**
     * Resource has degraded performance but is still operational.
     */
    DEGRADED("Degraded", "Resource operational with reduced performance"),
    
    /**
     * Resource validation failed.
     */
    VALIDATION_FAILED("Validation Failed", "Resource failed validation checks"),
    
    /**
     * Resource is inactive and not available for use.
     */
    INACTIVE("Inactive", "Resource is inactive"),
    
    /**
     * Resource has been deprecated and should not be used.
     */
    DEPRECATED("Deprecated", "Resource is deprecated"),
    
    /**
     * Resource has been permanently removed.
     */
    REMOVED("Removed", "Resource has been removed");
    
    private final String displayName;
    private final String description;
    
    ResourceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines if the resource is available for use.
     */
    public boolean isAvailable() {
        return this == ACTIVE || this == DEGRADED;
    }
    
    /**
     * Determines if the resource status indicates a temporary condition.
     */
    public boolean isTemporary() {
        return this == TEMPORARILY_UNAVAILABLE || this == MAINTENANCE || this == VALIDATING;
    }
    
    /**
     * Determines if the resource status indicates a permanent condition.
     */
    public boolean isPermanent() {
        return this == DEPRECATED || this == REMOVED || this == INACTIVE;
    }
    
    /**
     * Determines if the resource requires immediate attention.
     */
    public boolean requiresAttention() {
        return this == VALIDATION_FAILED || this == TEMPORARILY_UNAVAILABLE;
    }
    
    /**
     * Gets the next logical status for resource lifecycle management.
     */
    public ResourceStatus getNextStatus() {
        return switch (this) {
            case DISCOVERED -> VALIDATING;
            case VALIDATING -> ACTIVE;  // or VALIDATION_FAILED
            case TEMPORARILY_UNAVAILABLE -> ACTIVE;
            case MAINTENANCE -> ACTIVE;
            case DEGRADED -> ACTIVE;
            case VALIDATION_FAILED -> VALIDATING;
            default -> this;  // No automatic transition
        };
    }
}
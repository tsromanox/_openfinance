package br.com.openfinance.service.resources.domain;

/**
 * Enumeration of API endpoint operational status.
 */
public enum EndpointStatus {
    
    /**
     * Endpoint is active and responding normally.
     */
    ACTIVE("Active", "Endpoint is operational"),
    
    /**
     * Endpoint is responding but with degraded performance.
     */
    DEGRADED("Degraded", "Endpoint operational with reduced performance"),
    
    /**
     * Endpoint is temporarily unavailable.
     */
    UNAVAILABLE("Unavailable", "Endpoint temporarily unavailable"),
    
    /**
     * Endpoint is under maintenance.
     */
    MAINTENANCE("Maintenance", "Endpoint under maintenance"),
    
    /**
     * Endpoint is returning errors.
     */
    ERROR("Error", "Endpoint returning errors"),
    
    /**
     * Endpoint response time is too slow.
     */
    TIMEOUT("Timeout", "Endpoint response time exceeded"),
    
    /**
     * Endpoint has been deprecated.
     */
    DEPRECATED("Deprecated", "Endpoint is deprecated"),
    
    /**
     * Endpoint is no longer available.
     */
    REMOVED("Removed", "Endpoint has been removed");
    
    private final String displayName;
    private final String description;
    
    EndpointStatus(String displayName, String description) {
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
     * Determines if the endpoint is usable.
     */
    public boolean isUsable() {
        return this == ACTIVE || this == DEGRADED;
    }
    
    /**
     * Determines if the endpoint status is temporary.
     */
    public boolean isTemporary() {
        return this == UNAVAILABLE || this == MAINTENANCE || this == TIMEOUT;
    }
    
    /**
     * Determines if the endpoint requires attention.
     */
    public boolean requiresAttention() {
        return this == ERROR || this == TIMEOUT || this == UNAVAILABLE;
    }
}
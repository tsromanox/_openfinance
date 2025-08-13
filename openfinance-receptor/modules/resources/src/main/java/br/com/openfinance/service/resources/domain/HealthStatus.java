package br.com.openfinance.service.resources.domain;

/**
 * Enumeration of resource health status values.
 */
public enum HealthStatus {
    
    /**
     * Resource is operating normally.
     */
    UP("Up", "Resource is healthy and operational"),
    
    /**
     * Resource is experiencing issues but still operational.
     */
    DEGRADED("Degraded", "Resource operational with performance issues"),
    
    /**
     * Resource is not responding or unavailable.
     */
    DOWN("Down", "Resource is not available"),
    
    /**
     * Resource health status is unknown.
     */
    UNKNOWN("Unknown", "Resource health status cannot be determined");
    
    private final String displayName;
    private final String description;
    
    HealthStatus(String displayName, String description) {
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
     * Determines if the health status indicates the resource is usable.
     */
    public boolean isUsable() {
        return this == UP || this == DEGRADED;
    }
    
    /**
     * Determines if the health status requires attention.
     */
    public boolean requiresAttention() {
        return this == DOWN || this == DEGRADED;
    }
}
package br.com.openfinance.service.resources.domain;

/**
 * Enumeration of health indicator status values.
 */
public enum HealthIndicatorStatus {
    
    /**
     * Health indicator is in good state.
     */
    UP("Up", "Indicator is healthy"),
    
    /**
     * Health indicator shows warning levels.
     */
    WARNING("Warning", "Indicator shows warning levels"),
    
    /**
     * Health indicator is in critical state.
     */
    DOWN("Down", "Indicator is in critical state"),
    
    /**
     * Health indicator encountered an error.
     */
    ERROR("Error", "Indicator check failed with error"),
    
    /**
     * Health indicator status is unknown.
     */
    UNKNOWN("Unknown", "Indicator status cannot be determined");
    
    private final String displayName;
    private final String description;
    
    HealthIndicatorStatus(String displayName, String description) {
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
     * Determines if the status indicates a healthy state.
     */
    public boolean isHealthy() {
        return this == UP;
    }
    
    /**
     * Determines if the status requires attention.
     */
    public boolean requiresAttention() {
        return this == DOWN || this == ERROR || this == WARNING;
    }
}
package br.com.openfinance.service.resources.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a specific health indicator for a resource or endpoint.
 */
@Value
@Builder(toBuilder = true)
public class HealthIndicator {
    
    String name;
    HealthIndicatorStatus status;
    String message;
    String component;
    
    // Metrics and details
    Map<String, Object> details;
    double value;
    double threshold;
    String unit;
    
    // Temporal information
    LocalDateTime checkedAt;
    long checkDuration;  // milliseconds
    
    /**
     * Determines if this indicator is healthy.
     */
    public boolean isHealthy() {
        return status == HealthIndicatorStatus.UP;
    }
    
    /**
     * Determines if this indicator requires immediate attention.
     */
    public boolean requiresAttention() {
        return status == HealthIndicatorStatus.DOWN || status == HealthIndicatorStatus.ERROR;
    }
    
    /**
     * Creates a summary description of this health indicator.
     */
    public String getSummary() {
        if (unit != null && !unit.isEmpty()) {
            return String.format("%s: %.2f %s (threshold: %.2f %s) - %s", 
                    name, value, unit, threshold, unit, status.getDisplayName());
        } else {
            return String.format("%s: %s - %s", name, status.getDisplayName(), message);
        }
    }
    
    /**
     * Creates a new indicator with updated status and value.
     */
    public HealthIndicator withUpdatedStatus(HealthIndicatorStatus newStatus, double newValue, String newMessage) {
        return this.toBuilder()
                .status(newStatus)
                .value(newValue)
                .message(newMessage)
                .checkedAt(LocalDateTime.now())
                .build();
    }
}
package br.com.openfinance.service.resources.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a single metric measurement for a resource.
 */
@Value
@Builder(toBuilder = true)
public class ResourceMetric {
    
    String metricName;
    String metricType;
    double value;
    String unit;
    
    // Context and metadata
    String resourceId;
    String component;
    Map<String, String> tags;
    Map<String, Object> metadata;
    
    // Temporal information
    LocalDateTime timestamp;
    Duration measurementPeriod;
    
    /**
     * Creates a counter metric.
     */
    public static ResourceMetric counter(String name, long value, String resourceId) {
        return ResourceMetric.builder()
                .metricName(name)
                .metricType("counter")
                .value(value)
                .resourceId(resourceId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a gauge metric.
     */
    public static ResourceMetric gauge(String name, double value, String unit, String resourceId) {
        return ResourceMetric.builder()
                .metricName(name)
                .metricType("gauge")
                .value(value)
                .unit(unit)
                .resourceId(resourceId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a timer metric.
     */
    public static ResourceMetric timer(String name, long durationMs, String resourceId) {
        return ResourceMetric.builder()
                .metricName(name)
                .metricType("timer")
                .value(durationMs)
                .unit("milliseconds")
                .resourceId(resourceId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a histogram metric.
     */
    public static ResourceMetric histogram(String name, double value, String unit, String resourceId) {
        return ResourceMetric.builder()
                .metricName(name)
                .metricType("histogram")
                .value(value)
                .unit(unit)
                .resourceId(resourceId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a custom metric with specified type.
     */
    public static ResourceMetric custom(String name, String type, double value, String unit, String resourceId) {
        return ResourceMetric.builder()
                .metricName(name)
                .metricType(type)
                .value(value)
                .unit(unit)
                .resourceId(resourceId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Gets a formatted string representation of the metric.
     */
    public String getFormattedValue() {
        if (unit != null && !unit.isEmpty()) {
            return String.format("%.2f %s", value, unit);
        } else {
            return String.format("%.2f", value);
        }
    }
    
    /**
     * Gets a summary of the metric.
     */
    public String getSummary() {
        return String.format("%s[%s]: %s", metricName, metricType, getFormattedValue());
    }
    
    /**
     * Creates a copy with updated value.
     */
    public ResourceMetric withUpdatedValue(double newValue) {
        return this.toBuilder()
                .value(newValue)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a copy with additional tags.
     */
    public ResourceMetric withTags(Map<String, String> additionalTags) {
        Map<String, String> newTags = new java.util.HashMap<>();
        if (tags != null) {
            newTags.putAll(tags);
        }
        newTags.putAll(additionalTags);
        
        return this.toBuilder()
                .tags(newTags)
                .build();
    }
}
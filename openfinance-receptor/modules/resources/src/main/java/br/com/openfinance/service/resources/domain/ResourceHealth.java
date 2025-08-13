package br.com.openfinance.service.resources.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents the health status and metrics of an Open Finance resource.
 */
@Value
@Builder(toBuilder = true)
public class ResourceHealth {
    
    HealthStatus status;
    double healthScore;  // 0.0 to 1.0
    
    // Response time metrics
    long averageResponseTime;  // milliseconds
    long p95ResponseTime;      // milliseconds
    long p99ResponseTime;      // milliseconds
    
    // Availability metrics
    double uptime;             // percentage (0.0 to 1.0)
    long totalRequests;
    long successfulRequests;
    long failedRequests;
    double errorRate;          // percentage (0.0 to 1.0)
    
    // Service-specific health indicators
    List<HealthIndicator> indicators;
    Map<String, Object> details;
    
    // Temporal information
    LocalDateTime lastCheckAt;
    LocalDateTime healthPeriodStart;
    LocalDateTime healthPeriodEnd;
    
    /**
     * Determines if the resource is considered healthy.
     */
    public boolean isHealthy() {
        return status == HealthStatus.UP && healthScore >= 0.7;
    }
    
    /**
     * Determines if the resource has acceptable performance.
     */
    public boolean hasAcceptablePerformance() {
        return averageResponseTime <= 2000 && // 2 seconds
               p95ResponseTime <= 5000 &&     // 5 seconds
               errorRate <= 0.05;             // 5% error rate
    }
    
    /**
     * Determines if the resource is highly available.
     */
    public boolean isHighlyAvailable() {
        return uptime >= 0.99; // 99% uptime
    }
    
    /**
     * Gets the success rate as a percentage.
     */
    public double getSuccessRate() {
        if (totalRequests == 0) return 1.0;
        return (double) successfulRequests / totalRequests;
    }
    
    /**
     * Calculates an overall health score based on various metrics.
     */
    public double calculateOverallScore() {
        double availabilityScore = uptime;
        double performanceScore = calculatePerformanceScore();
        double reliabilityScore = getSuccessRate();
        
        // Weighted average
        return (availabilityScore * 0.4) + 
               (performanceScore * 0.3) + 
               (reliabilityScore * 0.3);
    }
    
    /**
     * Calculates performance score based on response times.
     */
    private double calculatePerformanceScore() {
        // Good performance: < 1000ms avg, < 2000ms p95
        // Acceptable: < 2000ms avg, < 5000ms p95
        // Poor: > 2000ms avg or > 5000ms p95
        
        if (averageResponseTime <= 1000 && p95ResponseTime <= 2000) {
            return 1.0; // Excellent
        } else if (averageResponseTime <= 2000 && p95ResponseTime <= 5000) {
            return 0.7; // Good
        } else if (averageResponseTime <= 5000 && p95ResponseTime <= 10000) {
            return 0.4; // Fair
        } else {
            return 0.1; // Poor
        }
    }
    
    /**
     * Gets a summary of health issues if any.
     */
    public List<String> getHealthIssues() {
        return indicators.stream()
                .filter(indicator -> indicator.getStatus() != HealthIndicatorStatus.UP)
                .map(HealthIndicator::getMessage)
                .toList();
    }
    
    /**
     * Creates a new health instance with updated metrics.
     */
    public ResourceHealth withUpdatedMetrics(long responseTime, boolean success) {
        long newTotalRequests = totalRequests + 1;
        long newSuccessfulRequests = success ? successfulRequests + 1 : successfulRequests;
        long newFailedRequests = success ? failedRequests : failedRequests + 1;
        
        // Simple moving average for response time
        long newAverageResponseTime = (averageResponseTime * totalRequests + responseTime) / newTotalRequests;
        
        double newErrorRate = (double) newFailedRequests / newTotalRequests;
        
        return this.toBuilder()
                .totalRequests(newTotalRequests)
                .successfulRequests(newSuccessfulRequests)
                .failedRequests(newFailedRequests)
                .averageResponseTime(newAverageResponseTime)
                .errorRate(newErrorRate)
                .lastCheckAt(LocalDateTime.now())
                .build();
    }
}
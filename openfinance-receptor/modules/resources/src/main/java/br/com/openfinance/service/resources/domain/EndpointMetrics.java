package br.com.openfinance.service.resources.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Performance and usage metrics for an API endpoint.
 */
@Value
@Builder(toBuilder = true)
public class EndpointMetrics {
    
    // Performance metrics
    long averageResponseTime;    // milliseconds
    long minResponseTime;        // milliseconds
    long maxResponseTime;        // milliseconds
    long p50ResponseTime;        // milliseconds
    long p95ResponseTime;        // milliseconds
    long p99ResponseTime;        // milliseconds
    
    // Usage metrics
    long totalRequests;
    long successfulRequests;
    long failedRequests;
    long timeoutRequests;
    
    // Rate metrics
    double requestsPerSecond;
    double errorRate;
    double timeoutRate;
    
    // Data transfer metrics
    long totalBytesTransferred;
    long averageResponseSize;
    
    // Temporal information
    LocalDateTime periodStart;
    LocalDateTime periodEnd;
    LocalDateTime lastUpdated;
    
    /**
     * Calculates the success rate as a percentage.
     */
    public double getSuccessRate() {
        if (totalRequests == 0) return 1.0;
        return (double) successfulRequests / totalRequests;
    }
    
    /**
     * Calculates the failure rate as a percentage.
     */
    public double getFailureRate() {
        if (totalRequests == 0) return 0.0;
        return (double) failedRequests / totalRequests;
    }
    
    /**
     * Determines if the endpoint has good performance.
     */
    public boolean hasGoodPerformance() {
        return averageResponseTime <= 1000 && // 1 second
               p95ResponseTime <= 2000 &&     // 2 seconds
               errorRate <= 0.01;             // 1% error rate
    }
    
    /**
     * Determines if the endpoint has acceptable performance.
     */
    public boolean hasAcceptablePerformance() {
        return averageResponseTime <= 3000 && // 3 seconds
               p95ResponseTime <= 5000 &&     // 5 seconds
               errorRate <= 0.05;             // 5% error rate
    }
    
    /**
     * Gets a performance score from 0.0 to 1.0.
     */
    public double getPerformanceScore() {
        double responseScore = calculateResponseScore();
        double reliabilityScore = getSuccessRate();
        
        // Weighted average
        return (responseScore * 0.6) + (reliabilityScore * 0.4);
    }
    
    /**
     * Calculates response time score.
     */
    private double calculateResponseScore() {
        if (averageResponseTime <= 500) return 1.0;      // Excellent
        if (averageResponseTime <= 1000) return 0.8;     // Good
        if (averageResponseTime <= 2000) return 0.6;     // Fair
        if (averageResponseTime <= 5000) return 0.3;     // Poor
        return 0.1; // Very poor
    }
    
    /**
     * Creates updated metrics with a new request measurement.
     */
    public EndpointMetrics withNewRequest(long responseTime, boolean successful, long responseSize) {
        long newTotal = totalRequests + 1;
        long newSuccessful = successful ? successfulRequests + 1 : successfulRequests;
        long newFailed = successful ? failedRequests : failedRequests + 1;
        
        // Update response time statistics
        long newMin = minResponseTime == 0 ? responseTime : Math.min(minResponseTime, responseTime);
        long newMax = Math.max(maxResponseTime, responseTime);
        long newAverage = (averageResponseTime * totalRequests + responseTime) / newTotal;
        
        // Update transfer statistics
        long newTotalBytes = totalBytesTransferred + responseSize;
        long newAverageSize = newTotalBytes / newTotal;
        
        // Update rates
        double newErrorRate = (double) newFailed / newTotal;
        
        return this.toBuilder()
                .totalRequests(newTotal)
                .successfulRequests(newSuccessful)
                .failedRequests(newFailed)
                .averageResponseTime(newAverage)
                .minResponseTime(newMin)
                .maxResponseTime(newMax)
                .totalBytesTransferred(newTotalBytes)
                .averageResponseSize(newAverageSize)
                .errorRate(newErrorRate)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
    
    /**
     * Gets a summary of the metrics.
     */
    public String getSummary() {
        return String.format("Requests: %d, Success: %.1f%%, Avg Response: %dms, P95: %dms", 
                totalRequests, getSuccessRate() * 100, averageResponseTime, p95ResponseTime);
    }
}
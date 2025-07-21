package br.com.openfinance.accounts.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateResult {

    private String executionId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now();

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime endTime;

    private Duration duration;

    private int totalProcessed;
    private int totalSuccess;
    private int totalErrors;
    private int totalSkipped;

    @Builder.Default
    private List<BatchResult> batchResults = new ArrayList<>();

    @Builder.Default
    private Map<String, Integer> errorsByType = new HashMap<>();

    @Builder.Default
    private Map<String, Integer> processingByInstitution = new HashMap<>();

    private ExecutionStatus status;
    private String message;

    @Builder.Default
    private PerformanceMetrics performanceMetrics = new PerformanceMetrics();

    public enum ExecutionStatus {
        COMPLETED,
        COMPLETED_WITH_ERRORS,
        FAILED,
        CANCELLED,
        IN_PROGRESS
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchResult {
        private int batchNumber;
        private int batchSize;
        private int successCount;
        private int errorCount;
        private long processingTimeMs;
        private LocalDateTime processedAt;

        public void incrementSuccess() {
            this.successCount++;
        }

        public void incrementError() {
            this.errorCount++;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private double averageProcessingTimeMs;
        private double p50ProcessingTimeMs;
        private double p95ProcessingTimeMs;
        private double p99ProcessingTimeMs;
        private double throughputPerSecond;
        private long peakMemoryUsageMb;
        private double averageCpuUsage;
    }

    public void addBatchResult(BatchResult batchResult) {
        this.batchResults.add(batchResult);
        this.totalSuccess += batchResult.getSuccessCount();
        this.totalErrors += batchResult.getErrorCount();
    }

    public void incrementErrorType(String errorType) {
        this.errorsByType.merge(errorType, 1, Integer::sum);
    }

    public void incrementInstitutionCount(String institutionId) {
        this.processingByInstitution.merge(institutionId, 1, Integer::sum);
    }

    public void complete() {
        this.endTime = LocalDateTime.now();
        this.duration = Duration.between(this.startTime, this.endTime);
        this.totalProcessed = this.totalSuccess + this.totalErrors + this.totalSkipped;

        if (this.totalErrors == 0) {
            this.status = ExecutionStatus.COMPLETED;
            this.message = String.format("Successfully processed %d accounts in %s",
                    totalProcessed, formatDuration(duration));
        } else {
            this.status = ExecutionStatus.COMPLETED_WITH_ERRORS;
            this.message = String.format("Processed %d accounts with %d errors in %s",
                    totalProcessed, totalErrors, formatDuration(duration));
        }

        calculatePerformanceMetrics();
    }

    public void fail(String reason) {
        this.endTime = LocalDateTime.now();
        this.duration = Duration.between(this.startTime, this.endTime);
        this.status = ExecutionStatus.FAILED;
        this.message = reason;
    }

    private void calculatePerformanceMetrics() {
        if (batchResults.isEmpty()) {
            return;
        }

        List<Long> processingTimes = batchResults.stream()
                .map(BatchResult::getProcessingTimeMs)
                .sorted()
                .toList();

        double average = processingTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        performanceMetrics.setAverageProcessingTimeMs(average);
        performanceMetrics.setP50ProcessingTimeMs(getPercentile(processingTimes, 50));
        performanceMetrics.setP95ProcessingTimeMs(getPercentile(processingTimes, 95));
        performanceMetrics.setP99ProcessingTimeMs(getPercentile(processingTimes, 99));

        if (duration != null && duration.toSeconds() > 0) {
            performanceMetrics.setThroughputPerSecond(
                    (double) totalProcessed / duration.toSeconds()
            );
        }
    }

    private double getPercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }

        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public static AccountUpdateResult inProgress(String executionId) {
        return AccountUpdateResult.builder()
                .executionId(executionId)
                .status(ExecutionStatus.IN_PROGRESS)
                .message("Update in progress...")
                .build();
    }
}

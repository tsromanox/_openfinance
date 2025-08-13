package br.com.openfinance.domain.processing;

public enum JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    DEAD_LETTER
}

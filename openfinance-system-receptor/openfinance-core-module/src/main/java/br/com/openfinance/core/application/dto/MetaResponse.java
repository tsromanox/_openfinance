package br.com.openfinance.core.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaResponse {
    private int totalRecords;
    private int totalPages;
    private LocalDateTime requestDateTime;
    private String xFapiInteractionId;
}

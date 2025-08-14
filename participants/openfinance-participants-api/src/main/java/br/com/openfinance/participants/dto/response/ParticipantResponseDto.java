package br.com.openfinance.participants.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponseDto {
    private String cnpj;
    private String organisationId;
    private String organisationName;
    private String legalEntityName;
    private String status;
    private Map<String, List<String>> apiEndpoints;
    private LocalDateTime lastUpdated;
}

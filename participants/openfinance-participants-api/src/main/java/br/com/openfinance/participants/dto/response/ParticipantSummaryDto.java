package br.com.openfinance.participants.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resumo das informações de um participante")
public class ParticipantSummaryDto {

    @Schema(
            description = "CNPJ do participante",
            example = "00000000000191"
    )
    private String cnpj;

    @Schema(
            description = "Nome da organização",
            example = "Banco do Brasil S.A."
    )
    private String organisationName;

    @Schema(
            description = "Razão social",
            example = "Banco do Brasil S.A."
    )
    private String legalEntityName;

    @Schema(
            description = "Status do participante",
            example = "Active"
    )
    private String status;

    @Schema(
            description = "Número total de endpoints de API disponíveis",
            example = "12"
    )
    private int totalApiEndpoints;

    @Schema(
            description = "Data da última atualização",
            example = "2024-01-15T10:30:00Z"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime lastUpdated;
}

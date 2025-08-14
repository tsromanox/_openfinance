package br.com.openfinance.participants.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resumo do participante")
public class ParticipantSummaryDto {

    @Schema(description = "CNPJ do participante", example = "00000000000191")
    private String cnpj;

    @Schema(description = "Nome da organização", example = "Banco do Brasil S.A.")
    private String organisationName;

    @Schema(description = "Nome da entidade legal", example = "Banco do Brasil S.A.")
    private String legalEntityName;

    @Schema(description = "Status do participante", example = "Active")
    private String status;

    @Schema(description = "Total de endpoints de API disponíveis", example = "10")
    private int totalApiEndpoints;

    @Schema(description = "Data da última atualização")
    private LocalDateTime lastUpdated;
}

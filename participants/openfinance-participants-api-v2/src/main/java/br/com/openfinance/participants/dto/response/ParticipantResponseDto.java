package br.com.openfinance.participants.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta com informações do participante")
public class ParticipantResponseDto {

    @Schema(description = "CNPJ do participante", example = "00000000000191")
    private String cnpj;

    @Schema(description = "ID da organização", example = "5f69111d-5cc3-43ea-ba97-30392654a505")
    private String organisationId;

    @Schema(description = "Nome da organização", example = "Banco do Brasil S.A.")
    private String organisationName;

    @Schema(description = "Nome da entidade legal", example = "Banco do Brasil S.A.")
    private String legalEntityName;

    @Schema(description = "Status do participante", example = "Active")
    private String status;

    @Schema(description = "Endpoints de API agrupados por família")
    private Map<String, List<String>> apiEndpoints;

    @Schema(description = "Data da última atualização")
    private LocalDateTime lastUpdated;
}

package br.com.openfinance.participants.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Informações de um endpoint de API de um participante")
public class ApiEndpointResponseDto {

    @Schema(
            description = "Família da API",
            example = "accounts",
            allowableValues = {"accounts", "payments", "resources", "consents", "customers"}
    )
    private String apiFamily;

    @Schema(
            description = "Versão da API",
            example = "2.0.0",
            pattern = "^\\d+\\.\\d+\\.\\d+$"
    )
    private String version;

    @Schema(
            description = "URL base do endpoint (protocolo + domínio)",
            example = "https://api.bb.com.br"
    )
    private String baseUrl;

    @Schema(
            description = "URL completa do endpoint da API",
            example = "https://api.bb.com.br/open-banking/accounts/v2"
    )
    private String fullEndpoint;

    @Schema(
            description = "Status da certificação da API",
            example = "Certified",
            allowableValues = {"Awaiting Certification", "Certified", "Deprecated", "Rejected", "Self-Certified"}
    )
    private String certificationStatus;
}

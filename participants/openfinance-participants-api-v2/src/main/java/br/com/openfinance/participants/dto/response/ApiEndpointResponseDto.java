package br.com.openfinance.participants.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta com endpoints de API")
public class ApiEndpointResponseDto {

    @Schema(description = "Família da API", example = "accounts")
    private String apiFamily;

    @Schema(description = "Versão da API", example = "2.4.2")
    private String version;

    @Schema(description = "URL base do endpoint", example = "https://api.bb.com.br")
    private String baseUrl;

    @Schema(description = "Endpoint completo", example = "https://api.bb.com.br/open-banking/accounts/v2")
    private String fullEndpoint;

    @Schema(description = "Status de certificação", example = "Certified")
    private String certificationStatus;
}

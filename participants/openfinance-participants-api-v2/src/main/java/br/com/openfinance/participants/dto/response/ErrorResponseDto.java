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
@Schema(description = "Resposta de erro")
public class ErrorResponseDto {

    @Schema(description = "Código do erro", example = "400")
    private int code;

    @Schema(description = "Mensagem de erro", example = "CNPJ inválido")
    private String message;

    @Schema(description = "Detalhes do erro")
    private String details;

    @Schema(description = "Timestamp do erro")
    private LocalDateTime timestamp;

    @Schema(description = "Path da requisição", example = "/api/v1/participants/123456")
    private String path;
}
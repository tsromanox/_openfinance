package br.com.openfinance.participants.exception;

import br.com.openfinance.participants.dto.response.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

/*    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Argumento inválido: {}", ex.getMessage());

        ErrorResponseDto error = ErrorResponseDto.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Requisição inválida")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.badRequest().body(error);
    }*/


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(
            IllegalArgumentException ex, ServerWebExchange exchange) { // FIX: Changed parameter to ServerWebExchange

        log.warn("Argumento inválido: {}", ex.getMessage());

        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Bad Request")
                .details(ex.getMessage())
                .path(exchange.getRequest().getPath().toString()) // FIX: Get path from ServerWebExchange
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        log.warn("Tipo de argumento inválido: {}", ex.getMessage());

        ErrorResponseDto error = ErrorResponseDto.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Tipo de parâmetro inválido")
                .details("O parâmetro '" + ex.getName() + "' possui um tipo inválido")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponseDto> handleWebClientResponseException(
            WebClientResponseException ex, WebRequest request) {

        log.error("Erro ao comunicar com API externa: {}", ex.getMessage());

        ErrorResponseDto error = ErrorResponseDto.builder()
                .code(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("Serviço temporariamente indisponível")
                .details("Erro ao comunicar com o diretório de participantes")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

/*    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Erro interno do servidor", ex);

        ErrorResponseDto error = ErrorResponseDto.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Erro interno do servidor")
                .details("Ocorreu um erro inesperado. Por favor, tente novamente mais tarde.")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.internalServerError().body(error);
    }*/

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception ex, ServerWebExchange exchange) { // FIX: Changed parameter to ServerWebExchange

        log.error("Erro interno do servidor", ex);

        ErrorResponseDto error = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Internal Server Error")
                .details("Ocorreu um erro inesperado. Por favor, tente novamente mais tarde.")
                .path(exchange.getRequest().getPath().toString()) // FIX: Get path from ServerWebExchange
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

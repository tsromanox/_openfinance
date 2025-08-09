package br.com.openfinance.core.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String code;
    private String title;
    private String detail;
    private int status;
    private LocalDateTime timestamp;
    private String path;
    private String traceId;
    private List<ErrorDetail> errors;
    private Map<String, String> validationErrors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String field;
        private String message;
        private String code;
    }
}

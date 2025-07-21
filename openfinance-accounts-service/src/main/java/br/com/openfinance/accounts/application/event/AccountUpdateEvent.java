package br.com.openfinance.accounts.application.event;

import br.com.openfinance.accounts.domain.entity.AccountBalance;
import br.com.openfinance.accounts.domain.entity.AccountLimit;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountUpdateEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private String eventType = "ACCOUNT_UPDATE";

    private String accountId;
    private String clientId;
    private String institutionId;
    private String consentId;

    private AccountBalance balance;
    private AccountLimit limit;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private UpdateStatus status;
    private String errorMessage;
    private String errorCode;

    @Builder.Default
    private EventMetadata metadata = new EventMetadata();

    public enum UpdateStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED,
        SKIPPED
    }

    @Data
    @Builder
    //@NoArgsConstructor
    @AllArgsConstructor
    public static class EventMetadata {
        private String correlationId;
        private String source;
        private Integer retryCount;
        private Long processingTimeMs;
        private String version;

        public EventMetadata() {
            this.correlationId = UUID.randomUUID().toString();
            this.source = "accounts-service";
            this.version = "1.0.0";
            this.retryCount = 0;
        }
    }

    public static AccountUpdateEvent success(String accountId, String clientId, String institutionId,
                                             AccountBalance balance, AccountLimit limit) {
        return AccountUpdateEvent.builder()
                .accountId(accountId)
                .clientId(clientId)
                .institutionId(institutionId)
                .balance(balance)
                .limit(limit)
                .status(UpdateStatus.SUCCESS)
                .build();
    }

    public static AccountUpdateEvent failure(String accountId, String clientId, String institutionId,
                                             String errorMessage, String errorCode) {
        return AccountUpdateEvent.builder()
                .accountId(accountId)
                .clientId(clientId)
                .institutionId(institutionId)
                .status(UpdateStatus.FAILED)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    public static AccountUpdateEvent partialSuccess(String accountId, String clientId, String institutionId,
                                                    AccountBalance balance, String errorMessage) {
        return AccountUpdateEvent.builder()
                .accountId(accountId)
                .clientId(clientId)
                .institutionId(institutionId)
                .balance(balance)
                .status(UpdateStatus.PARTIAL_SUCCESS)
                .errorMessage(errorMessage)
                .build();
    }
}

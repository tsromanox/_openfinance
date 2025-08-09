package br.com.openfinance.accounts.infrastructure.adapters.output.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("creditDebitType")
    private String creditDebitType;

    @JsonProperty("transactionName")
    private String transactionName;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("transactionCurrency")
    private String transactionCurrency;

    @JsonProperty("transactionDateTime")
    private LocalDateTime transactionDateTime;

    @JsonProperty("partieCnpjCpf")
    private String partieCnpjCpf;

    @JsonProperty("partiePersonType")
    private String partiePersonType;

    @JsonProperty("partieCompeCode")
    private String partieCompeCode;

    @JsonProperty("partieBranchCode")
    private String partieBranchCode;

    @JsonProperty("partieNumber")
    private String partieNumber;

    @JsonProperty("partieCheckDigit")
    private String partieCheckDigit;
}

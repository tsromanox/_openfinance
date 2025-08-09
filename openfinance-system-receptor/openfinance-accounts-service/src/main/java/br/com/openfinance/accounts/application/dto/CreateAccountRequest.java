package br.com.openfinance.accounts.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Participant ID is required")
    private String participantId;

    @NotBlank(message = "Brand ID is required")
    private String brandId;

    @NotBlank(message = "Account number is required")
    private String number;

    @NotBlank(message = "Check digit is required")
    private String checkDigit;

    @NotNull(message = "Account type is required")
    private String type;

    @NotNull(message = "Account subtype is required")
    private String subtype;

    private String currency = "BRL";

    private BigDecimal availableAmount = BigDecimal.ZERO;
    private BigDecimal blockedAmount = BigDecimal.ZERO;
    private BigDecimal automaticallyInvestedAmount = BigDecimal.ZERO;
}

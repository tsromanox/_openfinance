package br.com.openfinance.resources.application.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceMetadataDTO {
    private String currency;
    private BigDecimal balance;
    private BigDecimal availableAmount;
    private BigDecimal blockedAmount;
    private LocalDate dueDate;
    private LocalDate expiryDate;
    private String contractNumber;
    private Map<String, Object> additionalInfo;
}
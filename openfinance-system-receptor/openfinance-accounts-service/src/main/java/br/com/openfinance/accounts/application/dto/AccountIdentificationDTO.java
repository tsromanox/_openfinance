package br.com.openfinance.accounts.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountIdentificationDTO {
    private String ispb;
    private String issuer;
    private String number;
    private String checkDigit;
    private String accountType;
}

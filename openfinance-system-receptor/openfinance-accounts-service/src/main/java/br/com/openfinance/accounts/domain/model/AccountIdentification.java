package br.com.openfinance.accounts.domain.model;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountIdentification {
    private UUID identificationId;
    private Account account;
    private String ispb;
    private String issuer;
    private String number;
    private String checkDigit;
    private String accountType;
}

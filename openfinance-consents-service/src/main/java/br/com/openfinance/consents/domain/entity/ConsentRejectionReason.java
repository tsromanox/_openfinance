package br.com.openfinance.consents.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRejectionReason {
    private RejectionCode code;
    private String additionalInformation;
}

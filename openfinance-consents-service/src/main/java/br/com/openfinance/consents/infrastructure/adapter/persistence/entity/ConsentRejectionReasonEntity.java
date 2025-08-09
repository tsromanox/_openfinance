package br.com.openfinance.consents.infrastructure.adapter.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRejectionReasonEntity {
    private String code;
    private String additionalInformation;
}

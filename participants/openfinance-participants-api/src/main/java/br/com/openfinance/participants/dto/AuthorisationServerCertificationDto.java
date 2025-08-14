package br.com.openfinance.participants.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorisationServerCertificationDto {
    @JsonProperty("CertificationStartDate")
    private String certificationStartDate;

    @JsonProperty("CertificationExpirationDate")
    private String certificationExpirationDate;

    @JsonProperty("CertificationId")
    private String certificationId;

    @JsonProperty("AuthorisationServerId")
    private String authorisationServerId;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("ProfileVariant")
    private String profileVariant;

    @JsonProperty("ProfileVersion")
    private Double profileVersion;

    @JsonProperty("CertificationURI")
    private String certificationURI;
}

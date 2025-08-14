package br.com.openfinance.participants.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationAuthorityDomainClaimDto {
    @JsonProperty("OrganisationAuthorityDomainClaimId")
    private String organisationAuthorityDomainClaimId;

    @JsonProperty("AuthorisationDomainName")
    private String authorisationDomainName;

    @JsonProperty("AuthorityId")
    private String authorityId;

    @JsonProperty("AuthorityName")
    private String authorityName;

    @JsonProperty("RegistrationId")
    private String registrationId;

    @JsonProperty("Status")
    private String status;
}

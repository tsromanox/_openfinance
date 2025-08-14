package br.com.openfinance.participants.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationAuthorityClaimDto {
    @JsonProperty("OrganisationId")
    private String organisationId;

    @JsonProperty("OrganisationAuthorityClaimId")
    private String organisationAuthorityClaimId;

    @JsonProperty("AuthorityId")
    private String authorityId;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("AuthorisationDomain")
    private String authorisationDomain;

    @JsonProperty("Role")
    private String role;

    @JsonProperty("Authorisations")
    private List<AuthorisationDto> authorisations;

    @JsonProperty("RegistrationId")
    private String registrationId;
}

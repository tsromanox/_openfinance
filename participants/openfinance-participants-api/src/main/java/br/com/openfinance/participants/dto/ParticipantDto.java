package br.com.openfinance.participants.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDto {
    @JsonProperty("OrganisationId")
    private String organisationId;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("OrganisationName")
    private String organisationName;

    @JsonProperty("CreatedOn")
    private String createdOn;

    @JsonProperty("LegalEntityName")
    private String legalEntityName;

    @JsonProperty("CountryOfRegistration")
    private String countryOfRegistration;

    @JsonProperty("CompanyRegister")
    private String companyRegister;

    @JsonProperty("Tag")
    private String tag;

    @JsonProperty("Size")
    private String size;

    @JsonProperty("RegistrationNumber")
    private String registrationNumber;

    @JsonProperty("RegistrationId")
    private String registrationId;

    @JsonProperty("RegisteredName")
    private String registeredName;

    @JsonProperty("AddressLine1")
    private String addressLine1;

    @JsonProperty("AddressLine2")
    private String addressLine2;

    @JsonProperty("City")
    private String city;

    @JsonProperty("Postcode")
    private String postcode;

    @JsonProperty("Country")
    private String country;

    @JsonProperty("ParentOrganisationReference")
    private String parentOrganisationReference;

    @JsonProperty("AuthorisationServers")
    private List<AuthorisationServerDto> authorisationServers;

    @JsonProperty("OrgDomainClaims")
    private List<OrganisationAuthorityDomainClaimDto> orgDomainClaims;

    @JsonProperty("OrgDomainRoleClaims")
    private List<OrganisationAuthorityClaimDto> orgDomainRoleClaims;
}

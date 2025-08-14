package br.com.openfinance.participants.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorisationServerDto {
    @JsonProperty("AuthorisationServerId")
    private String authorisationServerId;

    @JsonProperty("OrganisationId")
    private String organisationId;

    @JsonProperty("AutoRegistrationSupported")
    private Boolean autoRegistrationSupported;

    @JsonProperty("SupportsCiba")
    private Boolean supportsCiba;

    @JsonProperty("SupportsDCR")
    private Boolean supportsDCR;

    @JsonProperty("ApiResources")
    private List<ApiResourceDto> apiResources;

    @JsonProperty("AuthorisationServerCertifications")
    private List<AuthorisationServerCertificationDto> authorisationServerCertifications;

    @JsonProperty("CustomerFriendlyDescription")
    private String customerFriendlyDescription;

    @JsonProperty("CustomerFriendlyLogoUri")
    private String customerFriendlyLogoUri;

    @JsonProperty("CustomerFriendlyName")
    private String customerFriendlyName;

    @JsonProperty("DeveloperPortalUri")
    private String developerPortalUri;

    @JsonProperty("TermsOfServiceUri")
    private String termsOfServiceUri;

    @JsonProperty("OpenIDDiscoveryDocument")
    private String openIDDiscoveryDocument;

    @JsonProperty("Issuer")
    private String issuer;

    @JsonProperty("PayloadSigningCertLocationUri")
    private String payloadSigningCertLocationUri;

    @JsonProperty("ParentAuthorisationServerId")
    private String parentAuthorisationServerId;
}

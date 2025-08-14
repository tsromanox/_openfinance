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
public class ApiResourceDto {
    @JsonProperty("ApiResourceId")
    private String apiResourceId;

    @JsonProperty("ApiVersion")
    private String apiVersion;

    @JsonProperty("ApiDiscoveryEndpoints")
    private List<ApiDiscoveryEndpointDto> apiDiscoveryEndpoints;

    @JsonProperty("FamilyComplete")
    private Boolean familyComplete;

    @JsonProperty("ApiCertificationUri")
    private String apiCertificationUri;

    @JsonProperty("CertificationStatus")
    private String certificationStatus;

    @JsonProperty("CertificationStartDate")
    private String certificationStartDate;

    @JsonProperty("CertificationExpirationDate")
    private String certificationExpirationDate;

    @JsonProperty("ApiFamilyType")
    private String apiFamilyType;
}

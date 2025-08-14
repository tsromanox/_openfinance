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
public class ApiDiscoveryEndpointDto {
    @JsonProperty("ApiDiscoveryId")
    private String apiDiscoveryId;

    @JsonProperty("ApiEndpoint")
    private String apiEndpoint;
}

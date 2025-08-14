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
public class ApiDiscoveryEndpointDto {
    @JsonProperty("ApiDiscoveryId")
    private String apiDiscoveryId;

    @JsonProperty("ApiEndpoint")
    private String apiEndpoint;
}

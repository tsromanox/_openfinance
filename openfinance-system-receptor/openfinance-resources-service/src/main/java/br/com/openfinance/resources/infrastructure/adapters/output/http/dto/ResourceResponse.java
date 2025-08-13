package br.com.openfinance.resources.infrastructure.adapters.output.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {
    @JsonProperty("resourceId")
    private String resourceId;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
    
    @JsonProperty("metadata")
    private ResourceMetadataResponse metadata;
}

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ResourceMetadataResponse {
    @JsonProperty("balance")
    private String balance;
    
    @JsonProperty("availableAmount")
    private String availableAmount;
    
    @JsonProperty("blockedAmount")
    private String blockedAmount;
    
    @JsonProperty("dueDate")
    private String dueDate;
    
    @JsonProperty("expiryDate")
    private String expiryDate;
    
    @JsonProperty("contractNumber")
    private String contractNumber;
}
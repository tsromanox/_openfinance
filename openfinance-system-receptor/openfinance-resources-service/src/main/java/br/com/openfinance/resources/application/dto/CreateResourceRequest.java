package br.com.openfinance.resources.application.dto;

import br.com.openfinance.resources.domain.model.ResourceType;
import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResourceRequest {
    @NotBlank(message = "External resource ID is required")
    private String externalResourceId;
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotBlank(message = "Participant ID is required")
    private String participantId;
    
    @NotBlank(message = "Brand ID is required")
    private String brandId;
    
    @NotNull(message = "Resource type is required")
    private ResourceType type;
    
    private String name;
    private String description;
    private ResourceMetadataDTO metadata;
}
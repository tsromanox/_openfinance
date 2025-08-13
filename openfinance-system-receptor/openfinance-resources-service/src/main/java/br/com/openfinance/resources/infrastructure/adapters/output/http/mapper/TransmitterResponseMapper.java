package br.com.openfinance.resources.infrastructure.adapters.output.http.mapper;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceMetadata;
import br.com.openfinance.resources.domain.model.ResourceType;
import br.com.openfinance.resources.domain.model.ResourceStatus;
import br.com.openfinance.resources.infrastructure.adapters.output.http.dto.ResourceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class TransmitterResponseMapper {

    public Resource toDomain(ResourceResponse response, String participantId, String customerId, String brandId) {
        if (response == null) {
            return null;
        }

        Resource.ResourceBuilder builder = Resource.builder()
                .externalResourceId(response.getResourceId())
                .participantId(participantId)
                .customerId(customerId)
                .brandId(brandId)
                .name(response.getName())
                .description(response.getDescription())
                .updatedAt(response.getUpdatedAt());

        // Map type
        if (response.getType() != null) {
            try {
                builder.type(ResourceType.fromCode(response.getType()));
            } catch (IllegalArgumentException e) {
                builder.type(ResourceType.ACCOUNT); // Default
            }
        }

        // Map status
        if (response.getStatus() != null) {
            try {
                builder.status(ResourceStatus.fromCode(response.getStatus()));
            } catch (IllegalArgumentException e) {
                builder.status(ResourceStatus.ACTIVE); // Default
            }
        }

        // Map metadata
        if (response.getMetadata() != null) {
            ResourceMetadata metadata = mapMetadata(response.getMetadata(), response.getCurrency());
            builder.metadata(metadata);
        }

        return builder.build();
    }

    private ResourceMetadata mapMetadata(Object metadataResponse, String currency) {
        if (metadataResponse == null) {
            return null;
        }

        ResourceMetadata.ResourceMetadataBuilder builder = ResourceMetadata.builder()
                .currency(currency);

        // This is a simplified mapping. In a real implementation,
        // you would need to properly handle the metadata response structure
        // based on the actual API response format

        return builder.build();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }
}
package br.com.openfinance.dto;

import java.util.List;

public record ApiResource(
        String ApiFamilyType,
        List<ApiDiscoveryEndpoint> ApiDiscoveryEndpoints
) {}

package br.com.openfinance.dto;

import java.util.List;

public record AuthorisationServer(
        String AuthorisationServerId,
        String CustomerFriendlyName,
        List<ApiResource> ApiResources
) {}

package br.com.openfinance.dto;

import java.util.List;

public record Participant(
        String OrganisationId,
        String OrganisationName,
        String RegistrationNumber,
        List<AuthorisationServer> AuthorisationServers
) {}


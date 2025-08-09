package br.com.openfinance.core.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${openfinance.api.version:v3}")
    private String apiVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Open Finance Brasil - " + applicationName)
                        .version(apiVersion)
                        .description("API do receptor de dados do Open Finance Brasil")
                        .contact(new Contact()
                                .name("Open Finance Team")
                                .email("openfinance@company.com.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("https://api.openfinance.com.br").description("Production"),
                        new Server().url("https://sandbox.openfinance.com.br").description("Sandbox"),
                        new Server().url("http://localhost:8080").description("Local")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authorization header using the Bearer scheme"))
                        .addSecuritySchemes("oauth2",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .description("OAuth2 Client Credentials Flow")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"));
    }
}

// OpenApiConfig.java
package br.com.openfinance.participants.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Participantes Open Finance Brasil")
                        .version("1.0.0")
                        .description("API para consulta de participantes e seus endpoints no ecossistema Open Finance Brasil. " +
                                "Esta API busca automaticamente as informações dos participantes a cada 2 horas e " +
                                "armazena em cache para consulta rápida.")
                        .contact(new Contact()
                                .name("Open Finance Brasil")
                                .url("https://openfinancebrasil.org.br")
                                .email("contato@openfinancebrasil.org.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort + "/participants").description("Servidor Local"),
                        new Server().url("https://api.exemplo.com.br/participants").description("Servidor de Produção")
                ));
    }
}

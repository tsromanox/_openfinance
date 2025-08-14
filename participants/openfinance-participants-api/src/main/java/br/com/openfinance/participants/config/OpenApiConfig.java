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

    @Value("${openapi.service.title:API de Participantes Open Finance Brasil}")
    private String serviceTitle;

    @Value("${openapi.service.version:1.0.0}")
    private String serviceVersion;

    @Value("${openapi.service.description:API para consulta de participantes do Open Finance Brasil}")
    private String serviceDescription;

    @Value("${openapi.service.url:http://localhost:8080}")
    private String serviceUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(serviceTitle)
                        .version(serviceVersion)
                        .description(serviceDescription +
                                "\n\n## 🚀 Funcionalidades Principais\n" +
                                "- 🔄 **Atualização Automática**: Cache atualizado a cada 2 horas\n" +
                                "- 🔍 **Busca por CNPJ**: Consulta participantes por CNPJ\n" +
                                "- 🌐 **URLs de API**: Extração automática de endpoints\n" +
                                "- 🏷️ **Filtros**: Filtrar por família de API (accounts, payments, etc.)\n" +
                                "- ⚡ **Alta Performance**: Cache em memória para resposta rápida\n" +
                                "- 📊 **Monitoramento**: Health checks e métricas integradas\n\n" +
                                "## 📡 Fonte de Dados\n" +
                                "Os dados são obtidos da API oficial do diretório de participantes do Open Finance Brasil:\n" +
                                "- **Produção**: https://data.directory.openbankingbrasil.org.br/participants\n" +
                                "- **Sandbox**: https://data.sandbox.directory.openbankingbrasil.org.br/participants\n\n" +
                                "## 🧪 Como Testar\n" +
                                "1. Expanda qualquer endpoint abaixo\n" +
                                "2. Clique em **Try it out**\n" +
                                "3. Preencha os parâmetros necessários\n" +
                                "4. Clique em **Execute** para ver a resposta real")
                        .contact(new Contact()
                                .name("Open Finance Brasil")
                                .email("contato@openfinancebrasil.org.br")
                                .url("https://openfinancebrasil.org.br"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url(serviceUrl)
                                .description("Servidor Local"),
                        new Server()
                                .url("https://api.openfinance.com.br")
                                .description("Servidor de Produção")));
    }
}

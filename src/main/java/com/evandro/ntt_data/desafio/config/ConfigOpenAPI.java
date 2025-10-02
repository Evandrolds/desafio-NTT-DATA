package com.evandro.ntt_data.desafio.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@OpenAPIDefinition(
        info = @io.swagger.v3.oas.annotations.info.Info(
                title = "🏦 Find My Agency API",
                version = "1.0.0",
                description = """
            API de geolocalização e cadastro de agências bancárias. 
            Permite encontrar as agências mais próximas com base na localização do usuário.
            """,
                contact = @io.swagger.v3.oas.annotations.info.Contact(
                        name = "Evandro Lima",
                        email = "evandro.lima@empresa.com",
                        url = "https://linkedin.com/in/evandrolima"
                ),
                license = @io.swagger.v3.oas.annotations.info.License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        externalDocs = @io.swagger.v3.oas.annotations.ExternalDocumentation(
                description = "Documentação Completa da API",
                url = "https://docs.meuapp.com/agencies-api"
        ),
        tags = {
                @Tag(name = "Agências", description = "Operações relacionadas a agências"),
                @Tag(name = "Localização", description = "Serviços de geolocalização"),
                @Tag(name = "Cadastro", description = "Gestão de cadastros")
        }
)
@Configuration
public class ConfigOpenAPI {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🏦 Find My Agency API")
                        .version("1.0.0")
                        .description("API para cadastro e localização de agências bancárias mais próximas")
                        .contact(new Contact()
                                .name("Evandro Lima")
                                .email("evandro.lima@empresa.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentação Completa")
                        .url("https://docs.meuapp.com/agencies-api"));
    }
}


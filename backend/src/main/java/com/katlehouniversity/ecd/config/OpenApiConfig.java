package com.katlehouniversity.ecd.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ECD Payment Reconciliation API")
                .version("1.0.0")
                .description("""
                    # ECD Payment Reconciliation System API

                    This API provides endpoints for managing Early Childhood Development (ECD) center payment reconciliation.

                    ## Features
                    - Student registration and management
                    - Automated payment matching from bank statements
                    - Real-time transaction notifications via webhook
                    - Monthly payment reports with PDF/Excel export
                    - Unmatched transaction review and manual assignment

                    ## Authentication
                    Most endpoints require JWT Bearer token authentication. Obtain a token by calling `/api/auth/login`.

                    ## Webhook Integration
                    The system can receive Standard Bank MyUpdates notifications via webhook at `/api/webhook/myupdates`.
                    Configure email forwarding using Zapier, Make.com, or Gmail Apps Script.

                    ## Rate Limiting
                    No rate limiting is currently enforced, but please use the API responsibly.
                    """)
                .contact(new Contact()
                    .name("Katlehong University ECD Center")
                    .email("support@katlehouniversity.ac.za")
                    .url("https://katlehouniversity.ac.za"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://katlehouniversity.ac.za/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local development server"),
                new Server()
                    .url("https://api.katlehouniversity.ac.za")
                    .description("Production server")
            ))
            .addSecurityItem(new SecurityRequirement()
                .addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT authentication token obtained from /api/auth/login"))
                .addSecuritySchemes("webhookApiKey",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                        .description("API key for webhook authentication")));
    }
}

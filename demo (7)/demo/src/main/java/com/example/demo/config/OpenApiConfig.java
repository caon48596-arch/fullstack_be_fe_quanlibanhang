package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ipworldOpenAPI() {
        final String bearerSchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("IPWORLD E-Commerce API")
                        .version("1.0.0")
                        .description("RESTful API cho hệ thống bán hàng IPWORLD (Modular Monolith: Spring Boot 3, MySQL 8 + Flyway, Redis, Order State Machine, Transactional Outbox, Payment Webhook).")
                        .contact(new Contact().name("IPWORLD Engineering")))
                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
                .components(new Components()
                        .addSecuritySchemes(bearerSchemeName, new SecurityScheme()
                                .name(bearerSchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

package com.example.alarms.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuration for Swagger. This is displayed at the top of Swagger page.
@SecurityScheme(
        name = "basicAuth", // A unique name for the scheme
        description = "Basic authentication for accessing the API",
        type = SecuritySchemeType.HTTP, // Specify the HTTP type
        scheme = "basic", // Use "basic" for Basic Authentication
        in = SecuritySchemeIn.HEADER // Indicate the authentication is passed in the header
)

@Configuration
public class SwaggerConfig {
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/**")
                .build();
    }
}
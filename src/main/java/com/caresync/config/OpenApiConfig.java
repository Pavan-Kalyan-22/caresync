package com.caresync.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.OAuthFlows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CareSync Backend API")
                        .version("1.0.0")
                        .description("Production-Grade Smart Healthcare Platform Backend API\n\n" +
                                "**Features:**\n" +
                                "- User Authentication with JWT\n" +
                                "- Email Verification with OTP\n" +
                                "- Password Reset Management\n" +
                                "- User Profile Management\n\n" +
                                "**Coming Soon:**\n" +
                                "- Weather-based Health Tracking\n" +
                                "- Water Intake Monitoring\n" +
                                "- AI Health Assistant\n" +
                                "- Emergency Services\n" +
                                "- Health Analytics Dashboard")
                        .contact(new Contact()
                                .name("CareSync Support")
                                .email("support@caresync.com")
                                .url("https://caresync.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")));
    }
}

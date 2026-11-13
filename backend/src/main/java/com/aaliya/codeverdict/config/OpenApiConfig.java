package com.aaliya.codeverdict.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui.html. Log in via /auth/login, then "Authorize" with the token. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI codeVerdictOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeVerdict API")
                        .version("1.0")
                        .description("AI code review that proves every bug with a real test."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}

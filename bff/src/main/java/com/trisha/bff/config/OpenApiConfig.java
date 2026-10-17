package com.trisha.bff.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bffOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Trilha BFF")
                .description("Backend for Frontend — agrega e roteia chamadas aos microservicos da Trilha.")
                .version("v1"));
    }
}

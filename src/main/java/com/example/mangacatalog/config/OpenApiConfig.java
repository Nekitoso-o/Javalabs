package com.example.mangacatalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Каталог Комиксов и Манги API")
                .version("1.0.0")
                .description("REST API для управления авторами, издателями, жанрами, комиксами и отзывами. " +
                    "Реализовано кэширование, пагинация и обработка проблемы N+1.")
                .contact(new Contact()
                    .name("Ваше Имя / Студент")));
    }
}
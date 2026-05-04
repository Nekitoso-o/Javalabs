package com.example.mangacatalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    // Разрешённые HTTP-методы для CORS
    private static final String[] ALLOWED_METHODS = {
        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    };

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            // CORS намеренно открыт для всех origins — учебный проект (dev-среда)
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")       // ← только /api/**, не все пути
                    .allowedOriginPatterns("*")
                    .allowedMethods(ALLOWED_METHODS) // ← явный список методов вместо "*"
                    .allowedHeaders("*")
                    .allowCredentials(false)          // ← явно false, не допускаем куки
                    .maxAge(3600);                    // ← кэш preflight на 1 час
            }
        };
    }
}
package com.example.mangacatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MangacatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(MangacatalogApplication.class, args);
    }
}